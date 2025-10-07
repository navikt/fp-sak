package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
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
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Addresse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.DokumentasjonPeriode;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.FamilieHendelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Familierelasjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Inntekt;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.PersonopplysningerForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.RelatertYtelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Virksomhet;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseStorrelse;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class PersonopplysningXmlTjenesteImpl extends PersonopplysningXmlTjeneste {
    private final no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory personopplysningBaseObjectFactory = new no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory();
    private final ObjectFactory personopplysningObjectFactory = new ObjectFactory();

    private FamilieHendelseRepository familieHendelseRepository;
    private MedlemskapRepository medlemskapRepository;
    private VergeRepository vergeRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private PersonopplysningXmlFelles personopplysningFellesTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

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
                                           VirksomhetTjeneste virksomhetTjeneste,
                                           ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        super(personopplysningTjeneste);
        this.personopplysningFellesTjeneste = fellesTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.familieHendelseRepository = provider.getFamilieHendelseRepository();
        this.medlemskapRepository = provider.getMedlemskapRepository();
        this.vergeRepository = vergeRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    @Override
    public Object lagPersonopplysning(PersonopplysningerAggregat personopplysningerAggregat, Long behandlingId, AktørId aktørId,
                                      Skjæringstidspunkt skjæringstidspunkter) {
        var personopplysninger = personopplysningObjectFactory.createPersonopplysningerForeldrepenger();
        familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId).ifPresent(familieHendelseGrunnlag -> {
            setVerge(behandlingId, personopplysninger);
            setMedlemskapsperioder(behandlingId, personopplysninger);
            setFamiliehendelse(personopplysninger, familieHendelseGrunnlag);
        });
        var skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        setAdresse(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setDokumentasjonsperioder(behandlingId, personopplysninger, skjæringstidspunkt);
        setInntekter(behandlingId, personopplysninger, skjæringstidspunkt);
        setBruker(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setFamilierelasjoner(skjæringstidspunkter, personopplysninger, personopplysningerAggregat);
        setRelaterteYtelser(behandlingId, aktørId, personopplysninger, skjæringstidspunkt);

        return personopplysningObjectFactory.createPersonopplysningerForeldrepenger(personopplysninger);
    }

    private void setFamiliehendelse(PersonopplysningerForeldrepenger personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var familieHendelse = personopplysningObjectFactory.createFamilieHendelse();
        setFoedsel(familieHendelse, familieHendelseGrunnlag);
        setAdopsjon(familieHendelse, familieHendelseGrunnlag);
        setTerminbekreftelse(familieHendelse, familieHendelseGrunnlag);
        personopplysninger.setFamiliehendelse(familieHendelse);
    }

    private void setRelaterteYtelser(Long behandlingId, AktørId aktørId, PersonopplysningerForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);
        var ytelser = ytelseFilter.getFiltrertYtelser();
        if (!ytelser.isEmpty()) {
            var relaterteYtelser = personopplysningObjectFactory
                .createPersonopplysningerForeldrepengerRelaterteYtelser();
            ytelser.forEach(ytelse -> relaterteYtelser.getRelatertYtelse().add(konverterFraDomene(ytelse)));
            personopplysninger.setRelaterteYtelser(relaterteYtelser);
        }
    }

    private RelatertYtelse konverterFraDomene(Ytelse ytelse) {
        var relatertYtelse = personopplysningObjectFactory.createRelatertYtelse();
        relatertYtelse.setKilde(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getKilde()));
        Optional.ofNullable(ytelse.getPeriode())
            .ifPresent(periode -> relatertYtelse.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getFomDato(), periode.getTomDato())));
        Optional.ofNullable(ytelse.getSaksnummer())
            .ifPresent(saksnummer -> relatertYtelse.setSaksnummer(VedtakXmlUtil.lagStringOpplysning(saksnummer.getVerdi())));
        relatertYtelse.setStatus(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getStatus()));
        relatertYtelse.setType(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getRelatertYtelseType()));

        setYtelseAnvist(relatertYtelse, ytelse.getYtelseAnvist());
        setYtelsesgrunnlag(relatertYtelse, ytelse.getYtelseGrunnlag());
        setYtelsesStørrelse(relatertYtelse, ytelse.getYtelseGrunnlag());
        return relatertYtelse;
    }

    private void setYtelsesStørrelse(RelatertYtelse relatertYtelseKontrakt, Optional<YtelseGrunnlag> ytelseGrunnlagDomene) {
        if (ytelseGrunnlagDomene.isPresent()) {
            var ytelseGrunnlag = ytelseGrunnlagDomene.get();
            var ytelseStorrelser = ytelseGrunnlag.getYtelseStørrelse().stream().map(this::konverterFraDomene)
                .toList();
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

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseGrunnlag konverterFraDomene(YtelseGrunnlag domene) {
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
            .map(this::konverterFraDomene).toList();
        relatertYtelseKontrakt.getYtelseanvist().addAll(alleYtelserAnvist);
    }

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseAnvist konverterFraDomene(YtelseAnvist domene) {
        var kontrakt = personopplysningObjectFactory.createYtelseAnvist();
        domene.getBeløp().ifPresent(beløp -> kontrakt.setBeloep(VedtakXmlUtil.lagDecimalOpplysning(beløp.getVerdi())));
        domene.getDagsats().ifPresent(dagsats -> kontrakt.setDagsats(VedtakXmlUtil.lagDecimalOpplysning(dagsats.getVerdi())));
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(domene.getAnvistFOM(), domene.getAnvistTOM()));
        domene.getUtbetalingsgradProsent().ifPresent(prosent -> kontrakt.setUtbetalingsgradprosent(VedtakXmlUtil.lagDecimalOpplysning(prosent.getVerdi())));

        return kontrakt;
    }

    private void setFamilierelasjoner(Skjæringstidspunkt stp, PersonopplysningerForeldrepenger personopplysninger, PersonopplysningerAggregat aggregat) {
        var aktørPersonopplysningMap = aggregat.getAktørPersonopplysningMap();
        var tilPersoner = aggregat.getSøkersRelasjoner().stream().filter(r -> aktørPersonopplysningMap.get(r.getTilAktørId()) != null).toList();
        if (!tilPersoner.isEmpty()) {
            var familierelasjoner = personopplysningObjectFactory.createPersonopplysningerForeldrepengerFamilierelasjoner();
            personopplysninger.setFamilierelasjoner(familierelasjoner);
            tilPersoner.forEach(relasjon -> personopplysninger.getFamilierelasjoner()
                .getFamilierelasjon()
                .add(lagRelasjon(stp, relasjon, aktørPersonopplysningMap.get(relasjon.getTilAktørId()), aggregat)));
        }
    }

    private Familierelasjon lagRelasjon(Skjæringstidspunkt stp, PersonRelasjonEntitet relasjon, PersonopplysningEntitet tilPerson, PersonopplysningerAggregat aggregat) {
        var familierelasjon = personopplysningObjectFactory.createFamilierelasjon();
        var person = personopplysningFellesTjeneste.lagBruker(stp, aggregat, tilPerson);
        familierelasjon.setTilPerson(person);
        familierelasjon.setRelasjon(VedtakXmlUtil.lagKodeverksOpplysning(relasjon.getRelasjonsrolle()));
        return familierelasjon;
    }

    private void setDokumentasjonsperioder(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {
        var dokumentasjonsperioder = personopplysningObjectFactory
            .createPersonopplysningerForeldrepengerDokumentasjonsperioder();

        foreldrepengerUttakTjeneste.hentHvisEksisterer(behandlingId).ifPresent(uttak -> {
            var perioder = uttak.getGjeldendePerioder()
                .stream()
                .filter(p -> p.getDokumentasjonVurdering().isPresent())
                .collect(Collectors.toSet());
            for (var periode : perioder) {
                var uttakDokumentasjonType = mapTilDokType(periode.getDokumentasjonVurdering().orElseThrow());
                //Bryr seg bare om perioder der dok er godkjent
                if (uttakDokumentasjonType != null) {
                    var dokPeriode = personopplysningObjectFactory.createDokumentasjonPeriode();
                    dokPeriode.setDokumentasjontype(uttakDokumentasjonType.tilKodeverksOpplysning());
                    dokPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getFom(), periode.getTom()));
                    dokumentasjonsperioder.getDokumentasjonperiode().add(dokPeriode);
                }
            }
        });

        ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId).ifPresent(aggregat -> {
            leggTilPerioderMedAleneomsorg(aggregat, dokumentasjonsperioder);
            if (!aggregat.harOmsorg()) {
                var dokumentasjonPeriode = personopplysningObjectFactory.createDokumentasjonPeriode();
                dokumentasjonPeriode.setDokumentasjontype(UttakDokumentasjonType.UTEN_OMSORG.tilKodeverksOpplysning());
                dokumentasjonPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(skjæringstidspunkt, skjæringstidspunkt.plusYears(3)));
                dokumentasjonsperioder.getDokumentasjonperiode().add(dokumentasjonPeriode);
            }
            if (Boolean.TRUE.equals(aggregat.getAnnenForelderRettAvklaring())) {
                dokumentasjonsperioder.getDokumentasjonperiode()
                    .addAll(lagEnkelDokumentasjonPeriode(UttakDokumentasjonType.ANNEN_FORELDER_HAR_RETT));
            }
            if (Boolean.TRUE.equals(aggregat.getAnnenForelderRettEØSAvklaring())) {
                dokumentasjonsperioder.getDokumentasjonperiode()
                    .addAll(lagEnkelDokumentasjonPeriode(UttakDokumentasjonType.ANNEN_FORELDER_RETT_EOS));
            }
            personopplysninger.setDokumentasjonsperioder(dokumentasjonsperioder);
        });
    }

    private static UttakDokumentasjonType mapTilDokType(DokumentasjonVurdering dokumentasjonVurdering) {
        return switch (dokumentasjonVurdering.type()) {
            case SYKDOM_SØKER_GODKJENT -> UttakDokumentasjonType.SYK_SØKER;
            case INNLEGGELSE_SØKER_GODKJENT -> UttakDokumentasjonType.INNLAGT_SØKER;
            case INNLEGGELSE_BARN_GODKJENT -> UttakDokumentasjonType.INNLAGT_BARN;
            case HV_OVELSE_GODKJENT -> UttakDokumentasjonType.HV_OVELSE;
            case NAV_TILTAK_GODKJENT -> UttakDokumentasjonType.NAV_TILTAK;
            case INNLEGGELSE_ANNEN_FORELDER_GODKJENT -> UttakDokumentasjonType.INSTITUSJONSOPPHOLD_ANNEN_FORELDRE;
            case SYKDOM_ANNEN_FORELDER_GODKJENT -> UttakDokumentasjonType.SYKDOM_ANNEN_FORELDER;
            case ALENEOMSORG_GODKJENT -> UttakDokumentasjonType.ALENEOMSORG_OVERFØRING;
            case BARE_SØKER_RETT_GODKJENT -> UttakDokumentasjonType.IKKE_RETT_ANNEN_FORELDER;
            case SYKDOM_SØKER_IKKE_GODKJENT, INNLEGGELSE_SØKER_IKKE_GODKJENT, BARE_SØKER_RETT_IKKE_GODKJENT, ALENEOMSORG_IKKE_GODKJENT,
                SYKDOM_ANNEN_FORELDER_IKKE_GODKJENT, INNLEGGELSE_ANNEN_FORELDER_IKKE_GODKJENT, TIDLIG_OPPSTART_FEDREKVOTE_IKKE_GODKJENT,
                TIDLIG_OPPSTART_FEDREKVOTE_GODKJENT, MORS_AKTIVITET_IKKE_DOKUMENTERT, MORS_AKTIVITET_IKKE_GODKJENT, MORS_AKTIVITET_GODKJENT,
                NAV_TILTAK_IKKE_GODKJENT, HV_OVELSE_IKKE_GODKJENT, INNLEGGELSE_BARN_IKKE_GODKJENT -> null;
        };
    }

    private void leggTilPerioderMedAleneomsorg(YtelseFordelingAggregat aggregat,
                                               PersonopplysningerForeldrepenger.Dokumentasjonsperioder dokumentasjonsperioder) {
        if (aggregat.harAleneomsorg()) {
            dokumentasjonsperioder.getDokumentasjonperiode()
                .addAll(lagEnkelDokumentasjonPeriode(UttakDokumentasjonType.ALENEOMSORG));
        }
    }

    private List<? extends DokumentasjonPeriode> lagEnkelDokumentasjonPeriode(UttakDokumentasjonType dokumentasjonType) {
        var dokumentasjonPeriode = personopplysningObjectFactory.createDokumentasjonPeriode();
        dokumentasjonPeriode.setDokumentasjontype(dokumentasjonType.tilKodeverksOpplysning());
        dokumentasjonPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(LocalDate.now(), LocalDate.now()));
        return List.of(dokumentasjonPeriode);
    }

    private void setInntekter(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {

        iayTjeneste.finnGrunnlag(behandlingId).ifPresent(grunnlag -> {
            var aktørInntekt = grunnlag.getAlleAktørInntektFraRegister();
            if (aktørInntekt != null) {
                var inntekter = personopplysningObjectFactory.createPersonopplysningerForeldrepengerInntekter();
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

        filter.forFilter((inntekt, inntektsposter) -> {
            var inntektXML = personopplysningObjectFactory.createInntekt();
            inntektsposter.forEach(inntektspost -> {
                var inntektspostXML = personopplysningObjectFactory.createInntektspost();
                if (inntekt.getArbeidsgiver() != null) {
                    inntektXML.setArbeidsgiver(VedtakXmlUtil.lagStringOpplysning(inntekt.getArbeidsgiver().getIdentifikator()));
                }
                inntektspostXML.setBeloep(VedtakXmlUtil.lagDoubleOpplysning(inntektspost.getBeløp().getVerdi().doubleValue()));
                var periode = VedtakXmlUtil.lagPeriodeOpplysning(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato());
                inntektspostXML.setPeriode(periode);
                inntektspostXML.setYtelsetype(VedtakXmlUtil.lagStringOpplysning(inntektspost.getInntektspostType().getKode()));
                inntektXML.getInntektsposter().add(inntektspostXML);
                inntektXML.setMottaker(VedtakXmlUtil.lagStringOpplysning(aktørId.getId()));
            });
            inntektList.add(inntektXML);
        });
        return inntektList;
    }

    private void setBruker(Skjæringstidspunkt stp, PersonopplysningerForeldrepenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var person = personopplysningFellesTjeneste.lagBruker(stp, personopplysningerAggregat, personopplysningerAggregat.getSøker());
        personopplysninger.setBruker(person);
    }

    private void setAdresse(Skjæringstidspunkt stp, PersonopplysningerForeldrepenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        var personopplysning = personopplysningerAggregat.getSøker();
        var opplysningAdresser = personopplysningerAggregat.getAdresserFor(personopplysning.getAktørId(), SimpleLocalDateInterval.enDag(stp.getUtledetSkjæringstidspunkt()));
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
        adresse.setLand(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getLand().getNavn()));
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

    private void setAdopsjon(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        familieHendelseGrunnlag.getGjeldendeAdopsjon().ifPresent(adopsjonhendelse -> {
            var adopsjon = personopplysningObjectFactory.createAdopsjon();
            if (adopsjonhendelse.isStebarnsadopsjon()) {
                adopsjon.setErEktefellesBarn(VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.isStebarnsadopsjon()));
            }
            familieHendelseGrunnlag.getGjeldendeBarna().forEach(aBarn -> adopsjon.getAdopsjonsbarn().add(leggTilAdopsjonsbarn(aBarn)));
            if (adopsjonhendelse.getAdoptererAlene() != null) {
                adopsjon.setAdoptererAlene(VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.getAdoptererAlene()));
            }
            if (adopsjonhendelse.getOmsorgsovertakelseDato() != null) {
                VedtakXmlUtil.lagDateOpplysning(adopsjonhendelse.getOmsorgsovertakelseDato()).ifPresent(adopsjon::setOmsorgsovertakelsesdato);
            }
            familieHendelse.setAdopsjon(adopsjon);
        });
    }

    private Adopsjon.Adopsjonsbarn leggTilAdopsjonsbarn(UidentifisertBarn aBarn) {
        var adopsjonsbarn = personopplysningObjectFactory.createAdopsjonAdopsjonsbarn();
        VedtakXmlUtil.lagDateOpplysning(aBarn.getFødselsdato()).ifPresent(adopsjonsbarn::setFoedselsdato);
        return adopsjonsbarn;
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

    private void setMedlemskapsperioder(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger) {
        medlemskapRepository.hentMedlemskap(behandlingId).ifPresent(medlemskapAggregat -> {
            var medlemskap = personopplysningObjectFactory.createMedlemskap();
            personopplysninger.setMedlemskap(medlemskap);

            medlemskapAggregat.getRegistrertMedlemskapPerioder()
                .forEach(medlemskapPeriode -> personopplysninger.getMedlemskap().getMedlemskapsperiode()
                    .add(personopplysningFellesTjeneste.lagMedlemskapPeriode(medlemskapPeriode)));
        });
    }

    private void setVerge(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger) {
        vergeRepository.hentAggregat(behandlingId).ifPresent(vergeAggregat -> vergeAggregat.getVerge().ifPresent(vergeFraBehandling -> {
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
        }));
    }

}
