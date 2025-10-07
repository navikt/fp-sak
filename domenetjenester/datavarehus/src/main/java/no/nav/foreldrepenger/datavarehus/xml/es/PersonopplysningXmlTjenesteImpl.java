package no.nav.foreldrepenger.datavarehus.xml.es;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Addresse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Familierelasjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Inntekt;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.PersonopplysningerEngangsstoenad;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
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
        var personopplysninger = personopplysningObjectFactory.createPersonopplysningerEngangsstoenad();
        var familieHendelseAggregatOptional = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        if (familieHendelseAggregatOptional.isPresent()) {
            var familieHendelseGrunnlag = familieHendelseAggregatOptional.get();
            setAdopsjon(personopplysninger, familieHendelseGrunnlag, personopplysningerAggregat);
            setFoedsel(personopplysninger, familieHendelseGrunnlag);
            setVerge(behandlingId,  personopplysninger);
            setMedlemskapsperioder(behandlingId, personopplysninger);
            setOmsorgsovertakelse(personopplysninger, familieHendelseGrunnlag);
            setTerminbekreftelse(personopplysninger, familieHendelseGrunnlag);
        }

        var skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        setAdresse(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setInntekter(behandlingId, personopplysninger, skjæringstidspunkt);
        setBruker(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setFamilierelasjoner(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setRelaterteYtelser(behandlingId, aktørId, personopplysninger, skjæringstidspunkt);
        return personopplysningObjectFactory.createPersonopplysningerEngangsstoenad(personopplysninger);
    }

    private void setRelaterteYtelser(Long behandlingId, AktørId aktørId, PersonopplysningerEngangsstoenad personopplysninger, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);
        var ytelser = ytelseFilter.getFiltrertYtelser();

        if (!ytelser.isEmpty()) {
            var relatertYtelse = personopplysningObjectFactory
                .createPersonopplysningerEngangsstoenadRelaterteYtelser();
            personopplysninger.setRelaterteYtelser(relatertYtelse);
        }
    }

    private void setTerminbekreftelse(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.TERMIN)) {
            var terminbekreftelseOptional = familieHendelseGrunnlag
                .getGjeldendeVersjon().getTerminbekreftelse();
            if (terminbekreftelseOptional.isPresent()) {
                var terminbekreftelseFraBehandling = terminbekreftelseOptional
                    .get();
                var terminbekreftelse = personopplysningObjectFactory.createTerminbekreftelse();
                terminbekreftelse.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(familieHendelseGrunnlag.getGjeldendeAntallBarn()));

                var utstedtDato = VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getUtstedtdato());
                utstedtDato.ifPresent(terminbekreftelse::setUtstedtDato);

                var terminDato = VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getTermindato());
                terminDato.ifPresent(terminbekreftelse::setTermindato);

                personopplysninger.setTerminbekreftelse(terminbekreftelse);
            }
        }
    }

    private void setOmsorgsovertakelse(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.OMSORG)) {
            var adopsjonOptional = familieHendelseGrunnlag
                .getGjeldendeVersjon().getAdopsjon();
            if (adopsjonOptional.isPresent()) {
                var adopsjonFraBehandling = adopsjonOptional.get();
                var omsorgsovertakelse = personopplysningObjectFactory.createOmsorgsovertakelse();

                var omsorgsovertakelsesDato = VedtakXmlUtil.lagDateOpplysning(adopsjonFraBehandling.getOmsorgsovertakelseDato());
                omsorgsovertakelsesDato.ifPresent(omsorgsovertakelse::setOmsorgsovertakelsesdato);
                personopplysninger.setOmsorgsovertakelse(omsorgsovertakelse);
            }
        }
    }

    private void setMedlemskapsperioder(Long behandlingId, PersonopplysningerEngangsstoenad personopplysninger) {
        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        if (medlemskap.isPresent()) {
            var medlemskapPerioderFraBehandling = medlemskap.get();

            var medlemskapsperioder = personopplysningObjectFactory
                .createPersonopplysningerEngangsstoenadMedlemskapsperioder();
            personopplysninger.setMedlemskapsperioder(medlemskapsperioder);
            medlemskapPerioderFraBehandling.getRegistrertMedlemskapPerioder()
                .forEach(medlemskapPeriode -> personopplysninger.getMedlemskapsperioder().getMedlemskapsperiode()
                    .add(personopplysningFellesTjeneste.lagMedlemskapPeriode(medlemskapPeriode)));
        }
    }

    private void setVerge(Long behandlingId, PersonopplysningerEngangsstoenad personopplysninger) {
        vergeRepository.hentAggregat(behandlingId).ifPresent(vergeAggregat -> vergeAggregat.getVerge().ifPresent(vergeFraBehandling -> {
            var verge = personopplysningObjectFactory.createVerge();
            if( vergeFraBehandling.getVergeOrganisasjon().isPresent()){
                verge.setNavn(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getNavn()));
                verge.setOrganisasjonsnummer(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getOrganisasjonsnummer()));
            }
            else {
                var aktørId = vergeAggregat.getAktørId();
                if (aktørId.isPresent()) {
                    verge.setNavn(VedtakXmlUtil.lagStringOpplysning(personopplysningFellesTjeneste.hentVergeNavn(aktørId.get())));
                }
            }
            verge.setVergetype(VedtakXmlUtil.lagKodeverksOpplysning(vergeFraBehandling.getVergeType()));
            verge.setGyldighetsperiode(VedtakXmlUtil.lagPeriodeOpplysning(vergeFraBehandling.getGyldigFom(), vergeFraBehandling.getGyldigTom()));
            personopplysninger.setVerge(verge);
        }));
    }

    private void setFoedsel(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var gjeldendeFamiliehendelse = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (Arrays.asList(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN).contains(gjeldendeFamiliehendelse.getType())) {

            var fødsel = personopplysningBaseObjectFactory.createFoedsel();

            fødsel.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(gjeldendeFamiliehendelse.getAntallBarn()));
            var fødselsdatoOptional = gjeldendeFamiliehendelse.getFødselsdato();
            if (fødselsdatoOptional.isPresent()) {
                var fødselsDato = VedtakXmlUtil.lagDateOpplysning(fødselsdatoOptional.get());
                fødselsDato.ifPresent(fødsel::setFoedselsdato);
            }

            personopplysninger.setFoedsel(fødsel);
        }
    }

    private void setFamilierelasjoner(Skjæringstidspunkt stp, PersonopplysningerEngangsstoenad personopplysninger, PersonopplysningerAggregat aggregat) {
        var aktørPersonopplysningMap = aggregat.getAktørPersonopplysningMap();
        var tilPersoner = aggregat.getSøkersRelasjoner().stream().filter(r -> aktørPersonopplysningMap.get(r.getTilAktørId()) != null).toList();
        if (!tilPersoner.isEmpty()) {
            var familierelasjoner = personopplysningObjectFactory.createPersonopplysningerEngangsstoenadFamilierelasjoner();
            personopplysninger.setFamilierelasjoner(familierelasjoner);
            tilPersoner.forEach(relasjon -> personopplysninger.getFamilierelasjoner()
                .getFamilierelasjon()
                .add(lagRelasjon(stp, relasjon, aktørPersonopplysningMap.get(relasjon.getTilAktørId()), aggregat)));
        }
    }

    private void setBruker(Skjæringstidspunkt stp, PersonopplysningerEngangsstoenad personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var person = personopplysningFellesTjeneste.lagBruker(stp, personopplysningerAggregat, personopplysningerAggregat.getSøker());
        personopplysninger.setBruker(person);
    }

    private void setInntekter(Long behandlingId, PersonopplysningerEngangsstoenad personopplysninger, LocalDate skjæringstidspunkt) {

        iayTjeneste.finnGrunnlag(behandlingId).ifPresent(grunnlag -> {
            var aktørInntekt = grunnlag.getAlleAktørInntektFraRegister();
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

    private List<Inntekt> lagInntekt(AktørId aktørId, InntektFilter filter) {
        List<Inntekt> inntektList = new ArrayList<>();
        filter.forFilter((inntekt, inntektsposter) -> inntektsposter.forEach(ip -> {
            var inntektXML = personopplysningObjectFactory.createInntekt();
            if (inntekt.getArbeidsgiver() != null) {
                inntektXML.setArbeidsgiver(VedtakXmlUtil.lagStringOpplysning(inntekt.getArbeidsgiver().getIdentifikator()));
            }
            inntektXML.setBeloep(VedtakXmlUtil.lagDoubleOpplysning(ip.getBeløp().getVerdi().doubleValue()));
            inntektXML.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()));
            inntektXML.setMottakerAktoerId(VedtakXmlUtil.lagStringOpplysning(aktørId.getId()));
            inntektXML.setYtelse(VedtakXmlUtil.lagBooleanOpplysning(ip.getInntektspostType().equals(InntektspostType.YTELSE)));
            inntektList.add(inntektXML);
        }));
        return inntektList;
    }

    private void setAdresse(Skjæringstidspunkt stp, PersonopplysningerEngangsstoenad personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var personopplysning = personopplysningerAggregat.getSøker();
        var opplysningAdresser = personopplysningerAggregat.getAdresserFor(personopplysning.getAktørId(), SimpleLocalDateInterval.enDag(stp.getUtledetSkjæringstidspunkt()));
        if (opplysningAdresser != null) {
            opplysningAdresser.forEach(addresse -> personopplysninger.getAdresse().add(lagAdresse(personopplysning, addresse)));
        }
    }

    private Addresse lagAdresse(PersonopplysningEntitet personopplysning, PersonAdresseEntitet adresseFraBehandling) {
        var adresse = personopplysningObjectFactory.createAddresse();
        adresse.setAddresseType(VedtakXmlUtil.lagKodeverksOpplysning(adresseFraBehandling.getAdresseType()));
        adresse.setAddresselinje1(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje1()));
        if (adresseFraBehandling.getAdresselinje2() != null) {
            adresse.setAddresselinje2(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje2()));
        }
        if (adresseFraBehandling.getAdresselinje3() != null) {
            adresse.setAddresselinje3(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje3()));
        }
        adresse.setLand(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getLand().getNavn()));
        adresse.setMottakersNavn(VedtakXmlUtil.lagStringOpplysning(personopplysning.getNavn()));
        adresse.setPostnummer(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getPostnummer()));
        return adresse;
    }

    private void setAdopsjon(PersonopplysningerEngangsstoenad personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag,
                             PersonopplysningerAggregat personopplysningerAggregat) {

        var adopsjonhendelseOptional = familieHendelseGrunnlag
            .getGjeldendeAdopsjon();
        if (adopsjonhendelseOptional.isPresent()) {

            var adopsjon = personopplysningObjectFactory.createAdopsjon();
            var adopsjonhendelse = adopsjonhendelseOptional.get();
            if (adopsjonhendelse.isStebarnsadopsjon()) {
                var erEktefellesBarn = VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.isStebarnsadopsjon());
                adopsjon.setErEktefellesBarn(erEktefellesBarn);
            }

            familieHendelseGrunnlag.getGjeldendeBarna()
                .forEach(aBarn -> adopsjon.getAdopsjonsbarn().add(leggTilAdopsjonsbarn(aBarn)));

            var erMann = NavBrukerKjønn.MANN.equals(personopplysningerAggregat.getSøker().getKjønn());
            if (erMann && adopsjonhendelse.getAdoptererAlene() != null) {
                adopsjon.setErMannAdoptererAlene(VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.getAdoptererAlene()));

            }
            if (adopsjonhendelse.getOmsorgsovertakelseDato() != null) {
                var omsorgOvertakelsesDato = VedtakXmlUtil.lagDateOpplysning(adopsjonhendelse.getOmsorgsovertakelseDato());
                omsorgOvertakelsesDato.ifPresent(adopsjon::setOmsorgsovertakelsesdato);
            }
            personopplysninger.setAdopsjon(adopsjon);
        }
    }

    private Adopsjon.Adopsjonsbarn leggTilAdopsjonsbarn(UidentifisertBarn aBarn) {
        var adopsjonAdopsjonsbarn = personopplysningObjectFactory.createAdopsjonAdopsjonsbarn();
        var dateOpplysning = VedtakXmlUtil.lagDateOpplysning(aBarn.getFødselsdato());
        dateOpplysning.ifPresent(adopsjonAdopsjonsbarn::setFoedselsdato);
        return adopsjonAdopsjonsbarn;
    }

    private Familierelasjon lagRelasjon(Skjæringstidspunkt stp, PersonRelasjonEntitet relasjon, PersonopplysningEntitet tilPerson, PersonopplysningerAggregat aggregat) {
        var familierelasjon = personopplysningObjectFactory.createFamilierelasjon();
        var person = personopplysningFellesTjeneste.lagBruker(stp, aggregat, tilPerson);
        familierelasjon.setTilPerson(person);
        familierelasjon.setRelasjon(VedtakXmlUtil.lagKodeverksOpplysning(relasjon.getRelasjonsrolle()));
        return familierelasjon;
    }

}
