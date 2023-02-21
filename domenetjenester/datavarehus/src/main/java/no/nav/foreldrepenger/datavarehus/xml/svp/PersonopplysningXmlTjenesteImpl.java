package no.nav.foreldrepenger.datavarehus.xml.svp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.datavarehus.xml.PersonopplysningXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.Addresse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.FamilieHendelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.Inntekt;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.Inntektspost;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.PersonopplysningerSvangerskapspenger;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.RelatertYtelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.Virksomhet;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.YtelseStorrelse;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class PersonopplysningXmlTjenesteImpl extends PersonopplysningXmlTjeneste {
    private final no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory personopplysningBaseObjectFactory = new no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory();
    private final ObjectFactory personopplysningObjectFactory = new ObjectFactory();

    private FamilieHendelseRepository familieHendelseRepository;
    private MedlemskapRepository medlemskapRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private PersonopplysningXmlFelles personopplysningFellesTjeneste;
    private VergeRepository vergeRepository;

    public PersonopplysningXmlTjenesteImpl() {
        // For CDI
    }

    @Inject
    public PersonopplysningXmlTjenesteImpl(PersonopplysningXmlFelles fellesTjeneste,
                                           BehandlingRepositoryProvider provider,
                                           PersonopplysningTjeneste personopplysningTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                           VergeRepository vergeRepository,
                                           VirksomhetTjeneste virksomhetTjeneste) {
        super(personopplysningTjeneste);
        this.personopplysningFellesTjeneste = fellesTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.familieHendelseRepository = provider.getFamilieHendelseRepository();
        this.medlemskapRepository = provider.getMedlemskapRepository();
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.vergeRepository = vergeRepository;
    }

    @Override
    public Object lagPersonopplysning(PersonopplysningerAggregat personopplysningerAggregat, Long behandlingId, AktørId aktørId,
                                      Skjæringstidspunkt skjæringstidspunkter) {
        var personopplysninger = personopplysningObjectFactory.createPersonopplysningerSvangerskapspenger();
        familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId).ifPresent(familieHendelseGrunnlag -> {
            setMedlemskapsperioder(behandlingId, personopplysninger);
            setFamiliehendelse(personopplysninger, familieHendelseGrunnlag);
            setVerge(behandlingId, personopplysninger);
        });
        var skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        setAdresse(personopplysninger, personopplysningerAggregat);
        setInntekter(behandlingId, personopplysninger, skjæringstidspunkt);
        setBruker(personopplysninger, personopplysningerAggregat);
        setRelaterteYtelser(behandlingId, aktørId, personopplysninger, skjæringstidspunkt);

        return personopplysningObjectFactory.createPersonopplysningerSvangerskapspenger(personopplysninger);
    }

    private void setFamiliehendelse(PersonopplysningerSvangerskapspenger personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var familieHendelse = personopplysningObjectFactory.createFamilieHendelse();
        setFoedsel(familieHendelse, familieHendelseGrunnlag);
        setTerminbekreftelse(familieHendelse, familieHendelseGrunnlag);
        personopplysninger.setFamiliehendelse(familieHendelse);
    }

    private void setRelaterteYtelser(Long behandlingId, AktørId aktørId, PersonopplysningerSvangerskapspenger personopplysninger,
                                     LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);

        if (!ytelseFilter.isEmpty()) {
            var relaterteYtelser = personopplysningObjectFactory
                .createPersonopplysningerSvangerskapspengerRelaterteYtelser();
            ytelseFilter.getFiltrertYtelser().stream().forEach(ytelse -> relaterteYtelser.getRelatertYtelse().add(konverterFraDomene(ytelse)));
            personopplysninger.setRelaterteYtelser(relaterteYtelser);
        }
    }

    private RelatertYtelse konverterFraDomene(Ytelse ytelse) {
        var relatertYtelse = personopplysningObjectFactory.createRelatertYtelse();
        relatertYtelse.setBehandlingstema(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getBehandlingsTema()));
        relatertYtelse.setKilde(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getKilde()));
        Optional.ofNullable(ytelse.getPeriode())
            .ifPresent(periode -> relatertYtelse.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getFomDato(), periode.getTomDato())));
        Optional.ofNullable(ytelse.getSaksnummer())
            .ifPresent(saksnummer -> relatertYtelse.setSaksnummer(VedtakXmlUtil.lagStringOpplysning(saksnummer.getVerdi())));
        relatertYtelse.setStatus(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getStatus()));
        relatertYtelse.setTemaUnderkategori(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getBehandlingsTema()));
        relatertYtelse.setType(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getRelatertYtelseType()));

        setYtelseAnvist(relatertYtelse, ytelse.getYtelseAnvist());
        setYtelsesgrunnlag(relatertYtelse, ytelse.getYtelseGrunnlag());
        setYtelsesStørrelse(relatertYtelse, ytelse.getYtelseGrunnlag());
        return relatertYtelse;
    }

    private void setYtelsesStørrelse(RelatertYtelse relatertYtelseKontrakt, Optional<YtelseGrunnlag> ytelseGrunnlagDomene) {
        if (ytelseGrunnlagDomene.isPresent()) {
            var ytelseGrunnlag = ytelseGrunnlagDomene.get();
            var ytelseStorrelser = ytelseGrunnlag.getYtelseStørrelse().stream().map(ys -> konverterFraDomene(ys))
                .collect(Collectors.toList());
            relatertYtelseKontrakt.getYtelsesstorrelse().addAll(ytelseStorrelser);
        }
    }

    private YtelseStorrelse konverterFraDomene(YtelseStørrelse domene) {
        var kontrakt = personopplysningObjectFactory.createYtelseStorrelse();
        domene.getOrgnr().flatMap(virksomhetTjeneste::finnOrganisasjon)
            .ifPresent(virksomhet -> kontrakt.setVirksomhet(tilVirksomhet(virksomhet)));
        kontrakt.setBeloep(VedtakXmlUtil.lagDecimalOpplysning(domene.getBeløp().getVerdi()));
        kontrakt.setHyppighet(VedtakXmlUtil.lagKodeverksOpplysning(domene.getHyppighet()));
        return kontrakt;
    }

    private Virksomhet tilVirksomhet(no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet domene) {
        var kontrakt = personopplysningObjectFactory.createVirksomhet();
        kontrakt.setNavn(VedtakXmlUtil.lagStringOpplysning(domene.getNavn()));
        kontrakt.setOrgnr(VedtakXmlUtil.lagStringOpplysning(domene.getOrgnr()));
        return kontrakt;
    }

    private void setYtelsesgrunnlag(RelatertYtelse relatertYtelseKontrakt, Optional<YtelseGrunnlag> ytelseGrunnlagDomene) {
        var ytelseGrunnlagOptional = ytelseGrunnlagDomene
            .map(this::konverterFraDomene);
        ytelseGrunnlagOptional.ifPresent(relatertYtelseKontrakt::setYtelsesgrunnlag);
    }

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.YtelseGrunnlag konverterFraDomene(YtelseGrunnlag domene) {
        var kontrakt = personopplysningObjectFactory.createYtelseGrunnlag();
        domene.getArbeidskategori()
            .ifPresent(arbeidskategori -> kontrakt.setArbeidtype(VedtakXmlUtil.lagKodeverksOpplysning(arbeidskategori)));
        domene.getDekningsgradProsent().ifPresent(dp -> kontrakt.setDekningsgradprosent(VedtakXmlUtil.lagDecimalOpplysning(dp.getVerdi())));

        domene.getGraderingProsent()
            .ifPresent(graderingsProsent -> kontrakt.setGraderingprosent(VedtakXmlUtil.lagDecimalOpplysning(graderingsProsent.getVerdi())));
        domene.getInntektsgrunnlagProsent().map(Stillingsprosent::getVerdi)
            .ifPresent(v -> kontrakt.setInntektsgrunnlagprosent(VedtakXmlUtil.lagDecimalOpplysning(v)));

        return kontrakt;
    }

    private void setYtelseAnvist(RelatertYtelse relatertYtelseKontrakt, Collection<YtelseAnvist> ytelseAnvistDomene) {
        var alleYtelserAnvist = ytelseAnvistDomene.stream()
            .map(this::konverterFraDomene).collect(Collectors.toList());
        relatertYtelseKontrakt.getYtelseanvist().addAll(alleYtelserAnvist);
    }

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.svp.v2.YtelseAnvist konverterFraDomene(YtelseAnvist domene) {
        var kontrakt = personopplysningObjectFactory.createYtelseAnvist();
        domene.getBeløp().ifPresent(beløp -> kontrakt.setBeloep(VedtakXmlUtil.lagDecimalOpplysning(beløp.getVerdi())));
        domene.getDagsats().ifPresent(dagsats -> kontrakt.setDagsats(VedtakXmlUtil.lagDecimalOpplysning(dagsats.getVerdi())));
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(domene.getAnvistFOM(), domene.getAnvistTOM()));
        domene.getUtbetalingsgradProsent().ifPresent(prosent -> kontrakt.setUtbetalingsgradprosent(VedtakXmlUtil.lagDecimalOpplysning(prosent.getVerdi())));

        return kontrakt;
    }

    private void setInntekter(Long behandlingId, PersonopplysningerSvangerskapspenger personopplysninger, LocalDate skjæringstidspunkt) {
        iayTjeneste.finnGrunnlag(behandlingId).ifPresent(grunnlag -> {
            var aktørInntekt = grunnlag.getAlleAktørInntektFraRegister();
            if (aktørInntekt != null) {
                var inntekter = personopplysningObjectFactory.createPersonopplysningerSvangerskapspengerInntekter();
                aktørInntekt.forEach(inntekt -> {
                    var filter = new InntektFilter(inntekt).før(skjæringstidspunkt).filterPensjonsgivende();
                    inntekter.getInntekt().addAll(lagInntekt(inntekt.getAktørId(), filter));
                    personopplysninger.setInntekter(inntekter);
                });
            }
        });
    }

    private Collection<? extends Inntekt> lagInntekt(AktørId aktørId, InntektFilter filter) {
        List<Inntekt> inntektList = new ArrayList<>();
        List<Inntektspost> inntektspostList = new ArrayList<>();

        filter.forFilter((inntekt, inntektsposter) -> {
            var inntektXML = personopplysningObjectFactory.createInntekt();
            inntektsposter.forEach(inntektspost -> {
                var inntektspostXML = personopplysningObjectFactory.createInntektspost();
                if (inntekt.getArbeidsgiver() != null) {
                    inntektXML.setArbeidsgiver(VedtakXmlUtil.lagStringOpplysning(inntekt.getArbeidsgiver().getIdentifikator()));
                }
                inntektspostXML.setBeloep(VedtakXmlUtil.lagDoubleOpplysning(inntektspost.getBeløp().getVerdi().doubleValue()));
                inntektspostXML.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato()));
                inntektspostXML.setYtelsetype(VedtakXmlUtil.lagStringOpplysning(inntektspost.getInntektspostType().getKode()));

                inntektXML.getInntektsposter().add(inntektspostXML);
                inntektXML.setMottaker(VedtakXmlUtil.lagStringOpplysning(aktørId.getId()));
                inntektspostList.add(inntektspostXML);
            });
            inntektList.add(inntektXML);
        });
        return inntektList;
    }

    private void setBruker(PersonopplysningerSvangerskapspenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var person = personopplysningFellesTjeneste.lagBruker(personopplysningerAggregat, personopplysningerAggregat.getSøker());
        personopplysninger.setBruker(person);
    }

    private void setAdresse(PersonopplysningerSvangerskapspenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        final var personopplysning = personopplysningerAggregat.getSøker();
        var opplysningAdresser = personopplysningerAggregat.getAdresserFor(personopplysning.getAktørId());
        if (opplysningAdresser != null) {
            opplysningAdresser.forEach(adresse -> personopplysninger.getAdresse().add(lagAdresse(personopplysning, adresse)));
        }
    }

    private Addresse lagAdresse(PersonopplysningEntitet personopplysning, PersonAdresseEntitet adresseFraBehandling) {
        var adresse = personopplysningObjectFactory.createAddresse();
        adresse.setAdressetype(VedtakXmlUtil.lagKodeverksOpplysning(adresseFraBehandling.getAdresseType()));
        adresse.setAddresselinje1(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje1()));
        if (adresseFraBehandling.getAdresselinje2() != null) {
            adresse.setAddresselinje2(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje2()));
        }
        if (adresseFraBehandling.getAdresselinje3() != null) {
            adresse.setAddresselinje3(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje3()));
        }
        if (adresseFraBehandling.getAdresselinje4() != null) {
            adresse.setAddresselinje4(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje4()));
        }
        adresse.setLand(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getLand()));
        adresse.setMottakersNavn(VedtakXmlUtil.lagStringOpplysning(personopplysning.getNavn()));
        adresse.setPostnummer(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getPostnummer()));
        return adresse;
    }

    private void setFoedsel(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var gjeldendeFamiliehendelse = familieHendelseGrunnlag
            .getGjeldendeVersjon();
        if (Arrays.asList(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN).contains(gjeldendeFamiliehendelse.getType())) {
            var fødsel = personopplysningBaseObjectFactory.createFoedsel();
            fødsel.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(gjeldendeFamiliehendelse.getAntallBarn()));
            gjeldendeFamiliehendelse.getFødselsdato().flatMap(VedtakXmlUtil::lagDateOpplysning).ifPresent(fødsel::setFoedselsdato);
            familieHendelse.setFoedsel(fødsel);
        }
    }

    private void setTerminbekreftelse(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.TERMIN)) {
            familieHendelseGrunnlag.getGjeldendeTerminbekreftelse().ifPresent(terminbekreftelseFraBehandling -> {
                var terminbekreftelse = personopplysningObjectFactory.createTerminbekreftelse();
                terminbekreftelse.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(familieHendelseGrunnlag.getGjeldendeAntallBarn()));
                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getUtstedtdato()).ifPresent(terminbekreftelse::setUtstedtDato);
                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getTermindato()).ifPresent(terminbekreftelse::setTermindato);
                familieHendelse.setTerminbekreftelse(terminbekreftelse);
            });
        }
    }

    private void setMedlemskapsperioder(Long behandlingId, PersonopplysningerSvangerskapspenger personopplysninger) {
        medlemskapRepository.hentMedlemskap(behandlingId).ifPresent(medlemskapAggregat -> {
            var medlemskap = personopplysningObjectFactory.createMedlemskap();
            personopplysninger.setMedlemskap(medlemskap);

            medlemskapAggregat.getRegistrertMedlemskapPerioder()
                .forEach(medlemskapPeriode -> personopplysninger.getMedlemskap().getMedlemskapsperiode()
                    .add(personopplysningFellesTjeneste.lagMedlemskapPeriode(medlemskapPeriode)));
        });
    }
    private void setVerge(Long behandlingId, PersonopplysningerSvangerskapspenger personopplysninger) {
        vergeRepository.hentAggregat(behandlingId).ifPresent(vergeAggregat -> {
            vergeAggregat.getVerge().ifPresent(vergeFraBehandling -> {
                var verge = personopplysningObjectFactory.createVerge();
                if( vergeFraBehandling.getVergeOrganisasjon().isPresent()){
                    verge.setNavn(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getNavn()));
                    verge.setOrganisasjonsnummer(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getOrganisasjonsnummer()));
                }
                else {
                    var aktørId = vergeAggregat.getAktørId();
                    aktørId.ifPresent(id -> verge.setNavn(VedtakXmlUtil.lagStringOpplysning(personopplysningFellesTjeneste.hentVergeNavn(id))));
                }
                verge.setVergetype(VedtakXmlUtil.lagKodeverksOpplysning(vergeFraBehandling.getVergeType()));
                verge.setGyldighetsperiode(VedtakXmlUtil.lagPeriodeOpplysning(vergeFraBehandling.getGyldigFom(), vergeFraBehandling.getGyldigTom()));
                personopplysninger.setVerge(verge);
            });
        });
    }
}
