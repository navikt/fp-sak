package no.nav.foreldrepenger.domene.vedtak.es;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.vedtak.felles.xml.felles.v2.BooleanOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.DateOpplysning;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Addresse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Familierelasjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Omsorgsovertakelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.PersonopplysningerEngangsstoenad;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Terminbekreftelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Verge;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.Foedsel;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.PersonIdentifiserbar;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class PersonopplysningXmlTjenesteImpl extends PersonopplysningXmlTjeneste {

    private final no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory personopplysningBaseObjectFactory = new no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory();
    private final ObjectFactory personopplysningObjectFactory = new ObjectFactory();
    private FamilieHendelseRepository familieHendelseRepository;
    private VergeRepository vergeRepository;
    private MedlemskapRepository medlemskapRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private PersonopplysningXmlFelles personopplysningFellesTjeneste;

    public PersonopplysningXmlTjenesteImpl() {
        // For CDI
    }

    @Inject
    public PersonopplysningXmlTjenesteImpl(PersonopplysningXmlFelles fellesTjeneste,
                                           BehandlingRepositoryProvider provider,
                                           PersonopplysningTjeneste personopplysningTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           VergeRepository vergeRepository) {
        super(personopplysningTjeneste);
        this.personopplysningFellesTjeneste = fellesTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.familieHendelseRepository = provider.getFamilieHendelseRepository();
        this.vergeRepository = vergeRepository;
        this.medlemskapRepository = provider.getMedlemskapRepository();
    }

    @Override
    public Object lagPersonopplysning(PersonopplysningerAggregat personopplysningerAggregat, Long behandlingId, AktørId aktørId,
                                      Skjæringstidspunkt skjæringstidspunkter) {
        PersonopplysningerEngangsstoenad personopplysninger = personopplysningObjectFactory.createPersonopplysningerEngangsstoenad();
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregatOptional = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (familieHendelseAggregatOptional.isPresent()) {
            FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieHendelseAggregatOptional.get();
            setAdopsjon(personopplysninger, familieHendelseGrunnlag, personopplysningerAggregat);
            setFoedsel(personopplysninger, familieHendelseGrunnlag);
            setVerge(behandlingId, personopplysninger);
            setMedlemskapsperioder(behandlingId, personopplysninger);
            setOmsorgsovertakelse(personopplysninger, familieHendelseGrunnlag);
            setTerminbekreftelse(personopplysninger, familieHendelseGrunnlag);
        }

        LocalDate skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        setAdresse(personopplysninger, personopplysningerAggregat);
        setInntekter(behandlingId, personopplysninger, skjæringstidspunkt);
        setBruker(personopplysninger, personopplysningerAggregat);
        setFamilierelasjoner(personopplysninger, personopplysningerAggregat);
        setRelaterteYtelser(behandlingId, aktørId, personopplysninger, skjæringstidspunkt);
        return personopplysningObjectFactory.createPersonopplysningerEngangsstoenad(personopplysninger);
    }

    private void setRelaterteYtelser(Long behandlingId, AktørId aktørId, PersonopplysningerEngangsstoenad personopplysninger, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);
        var ytelser = ytelseFilter.getFiltrertYtelser();

        if (!ytelser.isEmpty()) {
            PersonopplysningerEngangsstoenad.RelaterteYtelser relatertYtelse = personopplysningObjectFactory
                .createPersonopplysningerEngangsstoenadRelaterteYtelser();
            personopplysninger.setRelaterteYtelser(relatertYtelse);
        }
    }

    private void setTerminbekreftelse(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.TERMIN)) {
            Optional<no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet> terminbekreftelseOptional = familieHendelseGrunnlag
                .getGjeldendeVersjon().getTerminbekreftelse();
            if (terminbekreftelseOptional.isPresent()) {
                no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet terminbekreftelseFraBehandling = terminbekreftelseOptional
                    .get();
                Terminbekreftelse terminbekreftelse = personopplysningObjectFactory.createTerminbekreftelse();
                terminbekreftelse.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(familieHendelseGrunnlag.getGjeldendeAntallBarn()));

                Optional<DateOpplysning> utstedtDato = VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getUtstedtdato());
                utstedtDato.ifPresent(terminbekreftelse::setUtstedtDato);

                Optional<DateOpplysning> terminDato = VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getTermindato());
                terminDato.ifPresent(terminbekreftelse::setTermindato);

                personopplysninger.setTerminbekreftelse(terminbekreftelse);
            }
        }
    }

    private void setOmsorgsovertakelse(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.OMSORG)) {
            Optional<no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet> adopsjonOptional = familieHendelseGrunnlag
                .getGjeldendeVersjon().getAdopsjon();
            if (adopsjonOptional.isPresent()) {
                no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet adopsjonFraBehandling = adopsjonOptional.get();
                Omsorgsovertakelse omsorgsovertakelse = personopplysningObjectFactory.createOmsorgsovertakelse();

                Optional<DateOpplysning> omsorgsovertakelsesDato = VedtakXmlUtil.lagDateOpplysning(adopsjonFraBehandling.getOmsorgsovertakelseDato());
                omsorgsovertakelsesDato.ifPresent(omsorgsovertakelse::setOmsorgsovertakelsesdato);
                personopplysninger.setOmsorgsovertakelse(omsorgsovertakelse);
            }
        }
    }

    private void setMedlemskapsperioder(Long behandlingId, PersonopplysningerEngangsstoenad personopplysninger) {
        Optional<MedlemskapAggregat> medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        if (medlemskap.isPresent()) {
            MedlemskapAggregat medlemskapPerioderFraBehandling = medlemskap.get();

            PersonopplysningerEngangsstoenad.Medlemskapsperioder medlemskapsperioder = personopplysningObjectFactory
                .createPersonopplysningerEngangsstoenadMedlemskapsperioder();
            personopplysninger.setMedlemskapsperioder(medlemskapsperioder);
            medlemskapPerioderFraBehandling.getRegistrertMedlemskapPerioder()
                .forEach(medlemskapPeriode -> personopplysninger.getMedlemskapsperioder().getMedlemskapsperiode()
                    .add(personopplysningFellesTjeneste.lagMedlemskapPeriode(medlemskapPeriode)));
        }
    }

    private void setVerge(Long behandlingId, PersonopplysningerEngangsstoenad personopplysninger) {
        vergeRepository.hentAggregat(behandlingId).ifPresent(vergeAggregat -> {
            vergeAggregat.getVerge().ifPresent(vergeFraBehandling -> {
                Verge verge = personopplysningObjectFactory.createVerge();
                if( vergeFraBehandling.getVergeOrganisasjon().isPresent()){
                    verge.setNavn(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getNavn()));
                    verge.setOrganisasjonsnummer(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getOrganisasjonsnummer()));
                }
                else {
                    Optional<AktørId> aktørId = vergeAggregat.getAktørId();
                    if (aktørId.isPresent()) {
                        verge.setNavn(VedtakXmlUtil.lagStringOpplysning(personopplysningFellesTjeneste.hentVergeNavn(aktørId.get())));
                    }
                }
                verge.setVergetype(VedtakXmlUtil.lagKodeverksOpplysning(vergeFraBehandling.getVergeType()));
                verge.setGyldighetsperiode(VedtakXmlUtil.lagPeriodeOpplysning(vergeFraBehandling.getGyldigFom(), vergeFraBehandling.getGyldigTom()));
                personopplysninger.setVerge(verge);
            });
        });
    }

    private void setFoedsel(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        FamilieHendelseEntitet gjeldendeFamiliehendelse = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (Arrays.asList(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN).contains(gjeldendeFamiliehendelse.getType())) {

            Foedsel fødsel = personopplysningBaseObjectFactory.createFoedsel();

            fødsel.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(gjeldendeFamiliehendelse.getAntallBarn()));
            Optional<LocalDate> fødselsdatoOptional = gjeldendeFamiliehendelse.getFødselsdato();
            if (fødselsdatoOptional.isPresent()) {
                Optional<DateOpplysning> fødselsDato = VedtakXmlUtil.lagDateOpplysning(fødselsdatoOptional.get());
                fødselsDato.ifPresent(fødsel::setFoedselsdato);
            }

            personopplysninger.setFoedsel(fødsel);
        }
    }

    private void setFamilierelasjoner(PersonopplysningerEngangsstoenad personopplysninger, PersonopplysningerAggregat aggregat) {
        final Map<AktørId, PersonopplysningEntitet> aktørPersonopplysningMap = aggregat.getAktørPersonopplysningMap();
        final List<PersonRelasjonEntitet> tilPersoner = aggregat.getSøkersRelasjoner().stream()
            .filter(r -> aktørPersonopplysningMap.get(r.getTilAktørId()) != null)
            .collect(Collectors.toList());
        if (!tilPersoner.isEmpty()) {
            PersonopplysningerEngangsstoenad.Familierelasjoner familierelasjoner = personopplysningObjectFactory
                .createPersonopplysningerEngangsstoenadFamilierelasjoner();
            personopplysninger.setFamilierelasjoner(familierelasjoner);
            tilPersoner.forEach(relasjon -> personopplysninger.getFamilierelasjoner().getFamilierelasjon()
                .add(lagRelasjon(relasjon, aktørPersonopplysningMap.get(relasjon.getTilAktørId()), aggregat)));
        }
    }

    private void setBruker(PersonopplysningerEngangsstoenad personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        PersonIdentifiserbar person = personopplysningFellesTjeneste.lagBruker(personopplysningerAggregat, personopplysningerAggregat.getSøker());
        personopplysninger.setBruker(person);
    }

    private void setInntekter(Long behandlingId, PersonopplysningerEngangsstoenad personopplysninger, LocalDate skjæringstidspunkt) {

        iayTjeneste.finnGrunnlag(behandlingId).ifPresent(grunnlag -> {
            Collection<AktørInntekt> aktørInntekt = grunnlag.getAlleAktørInntektFraRegister();
            if (aktørInntekt != null) {
                var inntekter = personopplysningObjectFactory.createPersonopplysningerEngangsstoenadInntekter();
                aktørInntekt.forEach(inntekt -> {
                    var filter = new InntektFilter(inntekt).før(skjæringstidspunkt).filterPensjonsgivende();
                    inntekter.getInntekt().addAll(lagInntekt(inntekt.getAktørId(), filter));
                    personopplysninger.setInntekter(inntekter);
                });
            }
        });

    }

    private List<no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Inntekt> lagInntekt(AktørId aktørId, InntektFilter filter) {
        List<no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Inntekt> inntektList = new ArrayList<>();
        filter.forFilter((inntekt, inntektsposter) -> {
            inntektsposter.forEach(ip -> {
                no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Inntekt inntektXML = personopplysningObjectFactory.createInntekt(); // NOSONAR
                if (inntekt.getArbeidsgiver() != null) {
                    inntektXML.setArbeidsgiver(VedtakXmlUtil.lagStringOpplysning(inntekt.getArbeidsgiver().getIdentifikator()));
                }
                inntektXML.setBeloep(VedtakXmlUtil.lagDoubleOpplysning(ip.getBeløp().getVerdi().doubleValue()));
                inntektXML.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()));
                inntektXML.setMottakerAktoerId(VedtakXmlUtil.lagStringOpplysning(aktørId.getId()));
                inntektXML.setYtelse(VedtakXmlUtil.lagBooleanOpplysning(ip.getInntektspostType().equals(InntektspostType.YTELSE)));
                inntektList.add(inntektXML);
            });
        });
        return inntektList;
    }

    private void setAdresse(PersonopplysningerEngangsstoenad personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        final PersonopplysningEntitet personopplysning = personopplysningerAggregat.getSøker();
        List<PersonAdresseEntitet> opplysningAdresser = personopplysningerAggregat.getAdresserFor(personopplysning.getAktørId());
        if (opplysningAdresser != null) {
            opplysningAdresser.forEach(addresse -> personopplysninger.getAdresse().add(lagAdresse(personopplysning, addresse)));
        }
    }

    private Addresse lagAdresse(PersonopplysningEntitet personopplysning, PersonAdresseEntitet adresseFraBehandling) {
        Addresse adresse = personopplysningObjectFactory.createAddresse();
        adresse.setAddresseType(VedtakXmlUtil.lagKodeverksOpplysning(adresseFraBehandling.getAdresseType()));
        adresse.setAddresselinje1(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje1()));
        if (adresseFraBehandling.getAdresselinje2() != null) {
            adresse.setAddresselinje2(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje2()));
        }
        if (adresseFraBehandling.getAdresselinje3() != null) {
            adresse.setAddresselinje3(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje3()));
        }
        adresse.setLand(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getLand()));
        adresse.setMottakersNavn(VedtakXmlUtil.lagStringOpplysning(personopplysning.getNavn()));
        adresse.setPostnummer(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getPostnummer()));
        return adresse;
    }

    private void setAdopsjon(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag,
                             PersonopplysningerAggregat personopplysningerAggregat) {

        Optional<no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet> adopsjonhendelseOptional = familieHendelseGrunnlag
            .getGjeldendeAdopsjon();
        if (adopsjonhendelseOptional.isPresent()) {

            Adopsjon adopsjon = personopplysningObjectFactory.createAdopsjon();
            no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet adopsjonhendelse = adopsjonhendelseOptional.get();
            if (adopsjonhendelse.getErEktefellesBarn() != null) {
                BooleanOpplysning erEktefellesBarn = VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.getErEktefellesBarn());
                adopsjon.setErEktefellesBarn(erEktefellesBarn);
            }

            familieHendelseGrunnlag.getGjeldendeBarna()
                .forEach(aBarn -> adopsjon.getAdopsjonsbarn().add(leggTilAdopsjonsbarn(aBarn)));

            boolean erMann = NavBrukerKjønn.MANN.equals(personopplysningerAggregat.getSøker().getKjønn());
            if (erMann && adopsjonhendelse.getAdoptererAlene() != null) {
                adopsjon.setErMannAdoptererAlene(VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.getAdoptererAlene()));

            }
            if (adopsjonhendelse.getOmsorgsovertakelseDato() != null) {
                Optional<DateOpplysning> omsorgOvertakelsesDato = VedtakXmlUtil.lagDateOpplysning(adopsjonhendelse.getOmsorgsovertakelseDato());
                omsorgOvertakelsesDato.ifPresent(adopsjon::setOmsorgsovertakelsesdato);
            }
            personopplysninger.setAdopsjon(adopsjon);
        }
    }

    private Adopsjon.Adopsjonsbarn leggTilAdopsjonsbarn(UidentifisertBarn aBarn) {
        Adopsjon.Adopsjonsbarn adopsjonAdopsjonsbarn = personopplysningObjectFactory.createAdopsjonAdopsjonsbarn();
        Optional<DateOpplysning> dateOpplysning = VedtakXmlUtil.lagDateOpplysning(aBarn.getFødselsdato());
        dateOpplysning.ifPresent(adopsjonAdopsjonsbarn::setFoedselsdato);
        return adopsjonAdopsjonsbarn;
    }

    private Familierelasjon lagRelasjon(PersonRelasjonEntitet relasjon, PersonopplysningEntitet tilPerson, PersonopplysningerAggregat aggregat) {
        Familierelasjon familierelasjon = personopplysningObjectFactory.createFamilierelasjon();
        PersonIdentifiserbar person = personopplysningFellesTjeneste.lagBruker(aggregat, tilPerson);
        familierelasjon.setTilPerson(person);
        familierelasjon.setRelasjon(VedtakXmlUtil.lagKodeverksOpplysning(relasjon.getRelasjonsrolle()));
        return familierelasjon;
    }

}
