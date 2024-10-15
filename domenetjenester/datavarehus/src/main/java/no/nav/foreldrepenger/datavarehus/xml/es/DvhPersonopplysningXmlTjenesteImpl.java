package no.nav.foreldrepenger.datavarehus.xml.es;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.DvhPersonopplysningXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.es.v2.Addresse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.es.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.es.v2.Familierelasjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.es.v2.Inntekt;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.dvh.es.v2.PersonopplysningerDvhEngangsstoenad;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
@ApplicationScoped
public class DvhPersonopplysningXmlTjenesteImpl extends DvhPersonopplysningXmlTjeneste {

    private final no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory personopplysningBaseObjectFactory = new no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory();
    private final ObjectFactory personopplysningDvhObjectFactory = new ObjectFactory();
    private FamilieHendelseRepository familieHendelseRepository;
    private VergeRepository vergeRepository;
    private MedlemskapRepository medlemskapRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private PersonopplysningXmlFelles personopplysningFellesTjeneste;

    public DvhPersonopplysningXmlTjenesteImpl() {
        // CDI
    }

    @Inject
    public DvhPersonopplysningXmlTjenesteImpl(PersonopplysningXmlFelles fellesTjeneste,
                                                       FamilieHendelseRepository familieHendelseRepository,
                                                       VergeRepository vergeRepository,
                                                       MedlemskapRepository medlemskapRepository,
                                                       PersonopplysningTjeneste personopplysningTjeneste,
                                                       InntektArbeidYtelseTjeneste iayTjeneste) {
        super(personopplysningTjeneste);
        personopplysningFellesTjeneste = fellesTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.familieHendelseRepository = familieHendelseRepository;
        this.vergeRepository = vergeRepository;
        this.medlemskapRepository = medlemskapRepository;
    }

    @Override
    public Object lagPersonopplysning(PersonopplysningerAggregat personopplysningerAggregat, Long behandlingId, AktørId aktørId,
                                      Skjæringstidspunkt skjæringstidspunkter) {
        var personopplysninger = personopplysningDvhObjectFactory.createPersonopplysningerDvhEngangsstoenad();
        var familieHendelseAggregatOptional = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);

        familieHendelseAggregatOptional.ifPresent(familieHendelseGrunnlaghGr -> {
            setAdopsjon(personopplysninger, familieHendelseGrunnlaghGr);
            setFødsel(personopplysninger, familieHendelseGrunnlaghGr);
            setVerge(behandlingId, personopplysninger);
            setMedlemskapsperioder(behandlingId, personopplysninger);
            setOmsorgovertakelse(personopplysninger, familieHendelseGrunnlaghGr);
            setTerminbekreftelse(personopplysninger, familieHendelseGrunnlaghGr);
        });

        var skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        setAdresse(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setInntekter(behandlingId, personopplysninger, skjæringstidspunkt);
        setBruker(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setFamilierelasjoner(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setRelaterteYtelser(behandlingId, aktørId, personopplysninger, skjæringstidspunkt);

        return personopplysningDvhObjectFactory.createPersonopplysningerDvhEngangsstoenad(personopplysninger);
    }

    private void setRelaterteYtelser(Long behandlingId, AktørId aktørId, PersonopplysningerDvhEngangsstoenad personopplysninger, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);
        var ytelser = ytelseFilter.getFiltrertYtelser();
        if (!ytelser.isEmpty()) {
            var relaterteYtelse = personopplysningDvhObjectFactory
                .createPersonopplysningerDvhEngangsstoenadRelaterteYtelser();
            personopplysninger.setRelaterteYtelser(relaterteYtelse);
        }
    }

    private void setFamilierelasjoner(Skjæringstidspunkt stp, PersonopplysningerDvhEngangsstoenad personopplysninger, PersonopplysningerAggregat aggregat) {
        var aktørPersonopplysningMap = aggregat.getAktørPersonopplysningMap();
        var tilPersoner = aggregat.getSøkersRelasjoner().stream().filter(r -> aktørPersonopplysningMap.get(r.getTilAktørId()) != null).toList();
        if (!tilPersoner.isEmpty()) {
            var familierelasjoner = personopplysningDvhObjectFactory.createPersonopplysningerDvhEngangsstoenadFamilierelasjoner();
            personopplysninger.setFamilierelasjoner(familierelasjoner);
            tilPersoner.forEach(relasjon -> personopplysninger.getFamilierelasjoner()
                .getFamilierelasjon()
                .add(lagRelasjon(stp, relasjon, aktørPersonopplysningMap.get(relasjon.getTilAktørId()), aggregat)));
        }
    }

    private Familierelasjon lagRelasjon(Skjæringstidspunkt stp, PersonRelasjonEntitet relasjon, PersonopplysningEntitet tilPerson, PersonopplysningerAggregat aggregat) {
        var familierelasjon = personopplysningDvhObjectFactory.createFamilierelasjon();
        var person = personopplysningFellesTjeneste.lagUidentifiserbarBruker(stp, aggregat, tilPerson);
        familierelasjon.setTilPerson(person);
        familierelasjon.setRelasjon(VedtakXmlUtil.lagKodeverksOpplysning(relasjon.getRelasjonsrolle()));
        return familierelasjon;
    }

    private void setBruker(Skjæringstidspunkt stp, PersonopplysningerDvhEngangsstoenad personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var person = personopplysningFellesTjeneste.lagUidentifiserbarBruker(stp, personopplysningerAggregat, personopplysningerAggregat.getSøker());
        personopplysninger.setBruker(person);
    }

    private void setInntekter(Long behandlingId, PersonopplysningerDvhEngangsstoenad personopplysninger, LocalDate skjæringstidspunkt) {
        iayTjeneste.finnGrunnlag(behandlingId).ifPresent(grunnlag -> {
            var aktørInntekt = grunnlag.getAlleAktørInntektFraRegister();
            if (aktørInntekt != null) {
                var inntekterPersonopplysning = personopplysningDvhObjectFactory
                    .createPersonopplysningerDvhEngangsstoenadInntekter();
                aktørInntekt.forEach(inntekt -> {
                    var filter = new InntektFilter(inntekt).før(skjæringstidspunkt).filterPensjonsgivende();
                    inntekterPersonopplysning.getInntekt().addAll(lagInntekt(inntekt.getAktørId(), filter));
                });
                personopplysninger.setInntekter(inntekterPersonopplysning);
            }
        });
    }

    private List<? extends Inntekt> lagInntekt(AktørId aktørId, InntektFilter filter) {
        List<Inntekt> inntektList = new ArrayList<>();
        filter.forFilter((inntekt, inntektsposter) -> inntektsposter.forEach(inntektspost -> {
            var inntektXml = personopplysningDvhObjectFactory.createInntekt();
            if (inntekt.getArbeidsgiver() != null) {
                inntektXml.setArbeidsgiver(VedtakXmlUtil.lagStringOpplysning(inntekt.getArbeidsgiver().getIdentifikator()));
            }
            inntektXml.setBeloep(VedtakXmlUtil.lagDoubleOpplysning(inntektspost.getBeløp().getVerdi().doubleValue()));
            inntektXml.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato()));
            inntektXml.setMottakerAktoerId(VedtakXmlUtil.lagStringOpplysning(aktørId.getId()));
            inntektXml.setYtelse(VedtakXmlUtil.lagBooleanOpplysning(inntektspost.getInntektspostType().equals(InntektspostType.YTELSE)));
            inntektList.add(inntektXml);
        }));
        return inntektList;
    }

    private void setTerminbekreftelse(PersonopplysningerDvhEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.TERMIN)) {
            var terminbekreftelseOptional = familieHendelseGrunnlag
                .getGjeldendeTerminbekreftelse();
            terminbekreftelseOptional.ifPresent(terminbekreftelseFraBehandling -> {
                var terminbekreftelse = personopplysningDvhObjectFactory.createTerminbekreftelse();
                terminbekreftelse.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(familieHendelseGrunnlag.getGjeldendeAntallBarn()));

                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getUtstedtdato())
                    .ifPresent(terminbekreftelse::setUtstedtDato);

                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getTermindato()).ifPresent(terminbekreftelse::setTermindato);

                personopplysninger.setTerminbekreftelse(terminbekreftelse);
            });
        }
    }

    private void setOmsorgovertakelse(PersonopplysningerDvhEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.OMSORG)) {
            familieHendelseGrunnlag.getGjeldendeAdopsjon().ifPresent(adopsjonFraBehandling -> {
                var omsorgsovertakelse = personopplysningDvhObjectFactory.createOmsorgsovertakelse();

                VedtakXmlUtil.lagDateOpplysning(adopsjonFraBehandling.getOmsorgsovertakelseDato())
                    .ifPresent(omsorgsovertakelse::setOmsorgsovertakelsesdato);

                personopplysninger.setOmsorgsovertakelse(omsorgsovertakelse);
            });
        }
    }

    private void setMedlemskapsperioder(Long behandlingId, PersonopplysningerDvhEngangsstoenad personopplysninger) {
        medlemskapRepository.hentMedlemskap(behandlingId).ifPresent(medlemskapperioderFraBehandling -> {
            var medlemskapsperioder = personopplysningDvhObjectFactory
                .createPersonopplysningerDvhEngangsstoenadMedlemskapsperioder();
            personopplysninger.setMedlemskapsperioder(medlemskapsperioder);
            medlemskapperioderFraBehandling.getRegistrertMedlemskapPerioder()
                .forEach(medlemskapsperiode -> personopplysninger.getMedlemskapsperioder().getMedlemskapsperiode()
                    .add(personopplysningFellesTjeneste.lagMedlemskapPeriode(medlemskapsperiode)));
        });
    }

    private void setVerge(Long behandlingId, PersonopplysningerDvhEngangsstoenad personopplysninger) {
        vergeRepository.hentAggregat(behandlingId).flatMap(VergeAggregat::getVerge).ifPresent(vergeFraBehandling -> {
            var verge = personopplysningDvhObjectFactory.createVerge();
            verge.setVergetype(VedtakXmlUtil.lagKodeverksOpplysning(vergeFraBehandling.getVergeType()));
            verge.setGyldighetsperiode(VedtakXmlUtil.lagPeriodeOpplysning(vergeFraBehandling.getGyldigFom(), vergeFraBehandling.getGyldigTom()));

            // TODO(PJV): DVH må utvides med de nye feltene for organisasjon (og evt. adresse)

            personopplysninger.setVerge(verge);
        });
    }

    private void setFødsel(PersonopplysningerDvhEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var gjeldendeFamilieHendelse = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (Arrays.asList(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN).contains(gjeldendeFamilieHendelse.getType())) {
            var fødsel = personopplysningBaseObjectFactory.createFoedsel();
            fødsel.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(gjeldendeFamilieHendelse.getAntallBarn()));
            gjeldendeFamilieHendelse.getFødselsdato().flatMap(VedtakXmlUtil::lagDateOpplysning).ifPresent(fødsel::setFoedselsdato);
            personopplysninger.setFoedsel(fødsel);
        }
    }

    private void setAdopsjon(PersonopplysningerDvhEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        familieHendelseGrunnlag.getGjeldendeAdopsjon().ifPresent(adopsjonHendelse -> {
            var adopsjon = personopplysningDvhObjectFactory.createAdopsjon();
            if (adopsjonHendelse.getErEktefellesBarn() != null) {
                var erEktefellesBarn = VedtakXmlUtil.lagBooleanOpplysning(adopsjonHendelse.getErEktefellesBarn());
                adopsjon.setErEktefellesBarn(erEktefellesBarn);
            }

            familieHendelseGrunnlag.getGjeldendeBarna().forEach(aBarn -> adopsjon.getAdopsjonsbarn().add(leggTilAdopsjonsbarn(aBarn)));
            personopplysninger.setAdopsjon(adopsjon);
        });
    }

    private Adopsjon.Adopsjonsbarn leggTilAdopsjonsbarn(UidentifisertBarn aBarn) {
        var adopsjonsbarn = personopplysningDvhObjectFactory.createAdopsjonAdopsjonsbarn();
        VedtakXmlUtil.lagDateOpplysning(aBarn.getFødselsdato()).ifPresent(adopsjonsbarn::setFoedselsdato);
        return adopsjonsbarn;
    }

    private void setAdresse(Skjæringstidspunkt stp, PersonopplysningerDvhEngangsstoenad personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var personopplysning = personopplysningerAggregat.getSøker();
        var opplysningAdresser = personopplysningerAggregat.getAdresserFor(personopplysning.getAktørId(), SimpleLocalDateInterval.enDag(stp.getUtledetSkjæringstidspunkt()));
        if (opplysningAdresser != null) {
            opplysningAdresser.forEach(adresse -> personopplysninger.getAdresse().add(lagAdresse(adresse)));
        }
    }

    private Addresse lagAdresse(PersonAdresseEntitet adresseFraBehandling) {
        var adresse = personopplysningDvhObjectFactory.createAddresse();
        adresse.setAddresseType(VedtakXmlUtil.lagKodeverksOpplysning(adresseFraBehandling.getAdresseType()));
        adresse.setLand(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getLand().getNavn()));
        adresse.setPostnummer(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getPostnummer()));
        return adresse;
    }
}
