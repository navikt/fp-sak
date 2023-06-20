package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import static no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef.nullRef;
import static no.nav.foreldrepenger.web.app.tjenester.fpoversikt.SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode.ResultatÅrsak;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fpoversikt.FpSak.Uttaksperiode.Resultat;

@ApplicationScoped
public class FpOversiktDtoTjeneste {

    //TODO denne trenger oppdeling

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktDtoTjeneste.class);
    private static final Environment ENV = Environment.current();

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository vedtakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private FagsakRepository fagsakRepository;
    private MottatteDokumentRepository dokumentRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private UføretrygdRepository uføretrygdRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;

    @Inject
    public FpOversiktDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                 PersonopplysningTjeneste personopplysningTjeneste,
                                 FamilieHendelseTjeneste familieHendelseTjeneste,
                                 YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 UføretrygdRepository uføretrygdRepository,
                                 SvangerskapspengerRepository svangerskapspengerRepository,
                                 SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.dokumentRepository = repositoryProvider.getMottatteDokumentRepository();
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.svangerskapspengerUttakResultatRepository = svangerskapspengerUttakResultatRepository;
    }

    FpOversiktDtoTjeneste() {
        //CDI
    }

    public Sak hentSak(String saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        LOG.info("Henter sak {}", fagsak.getSaksnummer());
        var aktørId = fagsak.getAktørId().getId();

        var gjeldendeVedtak = finnGjeldendeVedtak(fagsak);
        var åpenYtelseBehandling = hentÅpenBehandling(fagsak);
        var familieHendelse = finnFamilieHendelse(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
        var erSakAvsluttet = erAvsluttet(fagsak);
        var ikkeHenlagteBehandlinger = finnIkkeHenlagteBehandlinger(fagsak);
        var aksjonspunkt = finnAksjonspunkt(ikkeHenlagteBehandlinger);
        var mottatteSøknader = finnRelevanteSøknadsdokumenter(fagsak);
        var alleVedtak = finnVedtakForFagsak(fagsak);
        return switch (fagsak.getYtelseType()) {
            case ENGANGSTØNAD -> {
                var søknader = finnEsSøknader(åpenYtelseBehandling, mottatteSøknader);
                var vedtak = finnEsVedtak(alleVedtak);
                yield new EsSak(saksnummer, aktørId, familieHendelse, erSakAvsluttet, aksjonspunkt, søknader, vedtak);
            }
            case FORELDREPENGER -> {
                var vedtak = finnFpVedtak(fagsak, alleVedtak);
                var oppgittAnnenPart = oppgittAnnenPart(fagsak).map(AktørId::getId).orElse(null);
                var søknader = finnFpSøknader(åpenYtelseBehandling, mottatteSøknader, fagsak);
                var gjeldendeBehandling = finnGjeldendeBehandling(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
                var fødteBarn = gjeldendeBehandling.map(this::finnFødteBarn).orElse(Set.of());
                var rettigheter = finnRettigheter(fagsak, gjeldendeVedtak, åpenYtelseBehandling);
                var ønskerJustertUttakVedFødsel = gjeldendeBehandling.flatMap(b -> ytelseFordelingTjeneste.hentAggregatHvisEksisterer(b.getId()))
                    .map(yfa -> yfa.getGjeldendeFordeling().ønskerJustertVedFødsel())
                    .orElse(false);
                var brukerRolle = finnBrukerRolle(fagsak);
                yield new FpSak(saksnummer, aktørId, familieHendelse, erSakAvsluttet, vedtak, oppgittAnnenPart, aksjonspunkt, søknader, brukerRolle,
                    fødteBarn, rettigheter.orElse(null), ønskerJustertUttakVedFødsel);
            }
            case SVANGERSKAPSPENGER -> {
                var søknader = finnSvpSøknader(åpenYtelseBehandling, mottatteSøknader);
                var vedtak = finnSvpVedtak(alleVedtak);
                yield new SvpSak(saksnummer, aktørId, familieHendelse, erSakAvsluttet, aksjonspunkt, søknader, vedtak);
            }
            case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + fagsak.getYtelseType());
        };
    }

    private Set<EsSak.Vedtak> finnEsVedtak(Stream<BehandlingVedtak> vedtak) {
        return vedtak.map(v -> new EsSak.Vedtak(v.getVedtakstidspunkt())).collect(Collectors.toSet());
    }

    private Set<SvpSak.Vedtak> finnSvpVedtak(Stream<BehandlingVedtak> vedtak) {
        //TODO Finnnes 12 saker i prod med br resultat opphør, men alle uttaksperioder er innvilget. Må håndtere disse
        return vedtak.map(v -> new SvpSak.Vedtak(v.getVedtakstidspunkt(), finnArbeidsforhold(v))).collect(Collectors.toSet());
    }

    private Set<SvpSak.Vedtak.ArbeidsforholdUttak> finnArbeidsforhold(BehandlingVedtak vedtak) {
        if (isProd()) {
            return Set.of();
        }
        var behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        return svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandlingId)
            .map(uttak -> {
                var tilretteleggingListe = svangerskapspengerRepository.hentGrunnlag(behandlingId).orElseThrow()
                    .getGjeldendeVersjon().getTilretteleggingListe();
                var uttaksResultatArbeidsforhold = uttak.getUttaksResultatArbeidsforhold();
                return uttaksResultatArbeidsforhold.stream()
                    .map(ua -> {
                        var type = mapTilAktivitetType(ua.getUttakArbeidType());
                        var arbeidsgiver = ua.getArbeidsgiver() == null ? null : new Arbeidsgiver(ua.getArbeidsgiver().getIdentifikator());
                        var arbeidsforholdId = ua.getArbeidsforholdRef() == null ? null : ua.getArbeidsforholdRef().getReferanse();
                        var ikkeOppfyltÅrsak = switch (ua.getArbeidsforholdIkkeOppfyltÅrsak()) {
                            case INGEN -> null;
                            case HELE_UTTAKET_ER_ETTER_3_UKER_FØR_TERMINDATO, UTTAK_KUN_PÅ_HELG -> SvpSak.Vedtak.ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ANNET;
                            case ARBEIDSGIVER_KAN_TILRETTELEGGE -> SvpSak.Vedtak.ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE;
                            case ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN -> SvpSak.Vedtak.ArbeidsforholdUttak.ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE_FREM_TIL_3_UKER_FØR_TERMIN;
                        };
                        var matchendeTilrettelegging = tilretteleggingListe.stream().filter(tl -> matcher(tl, ua)).findFirst();
                        var svpPerioder = ua.getPerioder().stream()
                            .map(p -> {
                                var matchendeTilretteleggingFOM = matchendeTilrettelegging.map(mt -> mt.getTilretteleggingFOMListe()).orElse(List.of()).stream()
                                    .sorted((o1, o2) -> o2.getFomDato().compareTo(o1.getFomDato()))
                                    .filter(tfom -> !tfom.getFomDato().isAfter(p.getFom()))
                                    .findFirst();
                                if (matchendeTilretteleggingFOM.isEmpty() && !p.getUtbetalingsgrad().harUtbetaling()) {
                                    //Uttaksperioder opprettet i fpsak. Ligger innvilget med og uten utbetaling i prdo
                                    return null;
                                }
                                var tilretteleggingType = matchendeTilretteleggingFOM.map(mt ->  mapTilretteleggingType(mt.getType()))
                                    .orElse(SvpSak.TilretteleggingType.INGEN);
                                var resultatÅrsak = switch (p.getPeriodeIkkeOppfyltÅrsak()) {
                                    case INGEN -> ResultatÅrsak.INNVILGET;
                                    case _8304, _8305, _8306 -> ResultatÅrsak.OPPHØR_ANNET;
                                    case _8308_SØKT_FOR_SENT -> ResultatÅrsak.AVSLAG_SØKNADSFRIST;
                                    case _8309 -> ResultatÅrsak.OPPHØR_FØDSEL;
                                    case _8310 -> ResultatÅrsak.OPPHØR_TIDSPERIODE_FØR_TERMIN;
                                    case _8314 -> ResultatÅrsak.OPPHØR_OVERGANG_FORELDREPENGER;
                                    case _8311, PERIODEN_ER_SAMTIDIG_SOM_SYKEPENGER -> ResultatÅrsak.AVSLAG_ANNET;
                                    case _8313 -> ResultatÅrsak.OPPHØR_OPPHOLD_I_YTELSEN;
                                    case SVANGERSKAPSVILKÅRET_IKKE_OPPFYLT, OPPTJENINGSVILKÅRET_IKKE_OPPFYLT -> ResultatÅrsak.AVSLAG_INNGANGSVILKÅR;
                                };
                                var arbeidstidprosent = matchendeTilretteleggingFOM.map(mt -> mt.getStillingsprosent())
                                    .orElse(BigDecimal.ZERO);
                                return new SvpSak.Vedtak.ArbeidsforholdUttak.SvpPeriode(p.getFom(), p.getTom(), tilretteleggingType, arbeidstidprosent, p.getUtbetalingsgrad().decimalValue(), resultatÅrsak);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                        var oppholdsperioder = matchendeTilrettelegging.map(mt -> oppholdsperioderFraTilrettelegging(mt)).orElse(Set.of());
                        return new SvpSak.Vedtak.ArbeidsforholdUttak(new SvpSak.Aktivitet(type, arbeidsgiver, arbeidsforholdId),
                            matchendeTilrettelegging.map(mt -> mt.getBehovForTilretteleggingFom()).orElse(null),
                            matchendeTilrettelegging.flatMap(SvpTilretteleggingEntitet::getOpplysningerOmRisikofaktorer).orElse(null),
                            matchendeTilrettelegging.flatMap(SvpTilretteleggingEntitet::getOpplysningerOmTilretteleggingstiltak).orElse(null),
                            svpPerioder, oppholdsperioder, ikkeOppfyltÅrsak);
                    }).collect(Collectors.toSet());
            })
            .orElse(Set.of());
    }

    private static Set<SvpSak.OppholdPeriode> oppholdsperioderFraTilrettelegging(SvpTilretteleggingEntitet matchendeTilrettelegging) {
        return matchendeTilrettelegging.getAvklarteOpphold()
            .stream()
            .map(o -> new SvpSak.OppholdPeriode(o.getFom(), o.getTom(), switch (o.getOppholdÅrsak()) {
                case SYKEPENGER -> SvpSak.OppholdPeriode.Årsak.SYKEPENGER;
                case FERIE -> SvpSak.OppholdPeriode.Årsak.FERIE;
            })).collect(Collectors.toSet());
    }

    private static SvpSak.Aktivitet.Type mapTilAktivitetType(UttakArbeidType uttakArbeidType) {
        return switch (uttakArbeidType) {
            case ORDINÆRT_ARBEID -> SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> SvpSak.Aktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> SvpSak.Aktivitet.Type.FRILANS;
            case ANNET -> throw new IllegalStateException(UNEXPECTED_VALUE + uttakArbeidType);
        };
    }

    private boolean matcher(SvpTilretteleggingEntitet tl, SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold) {
        var aktivitetType1 = mapTilAktivitetType(tl.getArbeidType());
        var aktivitetType2 = mapTilAktivitetType(arbeidsforhold.getUttakArbeidType());

        return aktivitetType1 == aktivitetType2 &&
            Objects.equals(arbeidsforhold.getArbeidsgiver(), tl.getArbeidsgiver().orElse(null)) &&
            Objects.equals(arbeidsforhold.getArbeidsforholdRef(), tl.getInternArbeidsforholdRef().orElse(nullRef()));
    }

    private Optional<BehandlingVedtak> finnGjeldendeVedtak(Fagsak fagsak) {
        var gjeldendeVedtak = vedtakRepository.hentGjeldendeVedtak(fagsak);
        gjeldendeVedtak.ifPresentOrElse(v -> LOG.info("Fant gjeldende vedtak for sak {} {}", fagsak.getSaksnummer(), v.getId()),
            () -> LOG.info("Fant ikke et vedtak for sak {}", fagsak.getSaksnummer()));
        return gjeldendeVedtak;
    }

    private Optional<FpSak.Rettigheter> finnRettigheter(Fagsak fagsak, Optional<BehandlingVedtak> gjeldendeVedtak, Optional<Behandling> åpenYtelseBehandling) {
        if (gjeldendeVedtak.isPresent()) {
            return finnGjeldendeRettigheter(gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId());
        }
        if (åpenYtelseBehandling.isPresent()) {
            return finnOppgitteRettigheter(åpenYtelseBehandling.get().getId());
        }
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        return sisteBehandling.flatMap(b -> finnOppgitteRettigheter(b.getId()));
    }

    private Optional<FpSak.Rettigheter> finnOppgitteRettigheter(Long behandlingId) {
       return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId).map(ytelseFordelingAggregat -> {
           var oppgittRettighet = ytelseFordelingAggregat.getOppgittRettighet();
           var aleneomsorg = oppgittRettighet.getHarAleneomsorgForBarnet();
           var annenForelderRettEØS = oppgittRettighet.getAnnenForelderRettEØS();
           var morUføretrygd = oppgittRettighet.getMorMottarUføretrygd();
           return new FpSak.Rettigheter(aleneomsorg, morUføretrygd, annenForelderRettEØS);
       });
    }

    private Optional<FpSak.Rettigheter> finnGjeldendeRettigheter(Long behandlingId) {
        return ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId).map(ytelseFordelingAggregat -> {
            var aleneomsorg = UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat);
            var annenForelderRettEØS = UttakOmsorgUtil.avklartAnnenForelderHarRettEØS(ytelseFordelingAggregat);
            var uføretrygdGrunnlagEntitet = uføretrygdRepository.hentGrunnlag(behandlingId);
            var morUføretrygd = UttakOmsorgUtil.morMottarUføretrygd(ytelseFordelingAggregat, uføretrygdGrunnlagEntitet.orElse(null));
            return new FpSak.Rettigheter(aleneomsorg, morUføretrygd, annenForelderRettEØS);
        });
    }

    private Optional<Behandling> finnGjeldendeBehandling(Fagsak fagsak, Optional<BehandlingVedtak> gjeldendeVedtak, Optional<Behandling> åpenYtelseBehandling) {
        final Optional<Behandling> behandling;
        if (gjeldendeVedtak.isPresent()) {
            var behandlingId = gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId();
            behandling = Optional.of(behandlingRepository.hentBehandling(behandlingId));
        } else if (åpenYtelseBehandling.isPresent()) {
            behandling = åpenYtelseBehandling;
        } else {
            behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        }
        return behandling;
    }

    private Set<String> finnFødteBarn(Behandling behandling) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .map(pi -> pi.getBarna().stream().map(barn -> barn.getAktørId().getId()).collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    private FpSak.BrukerRolle finnBrukerRolle(Fagsak fagsak) {
        return switch (fagsak.getRelasjonsRolleType()) {
            case FARA -> FpSak.BrukerRolle.FAR;
            case MORA -> FpSak.BrukerRolle.MOR;
            case MEDMOR -> FpSak.BrukerRolle.MEDMOR;
            case UDEFINERT -> FpSak.BrukerRolle.UKJENT;
            case EKTE, REGISTRERT_PARTNER, BARN, ANNEN_PART_FRA_SØKNAD -> throw new IllegalStateException(UNEXPECTED_VALUE + fagsak.getRelasjonsRolleType());
        };
    }

    private List<MottattDokument> finnRelevanteSøknadsdokumenter(Fagsak fagsak) {
        return dokumentRepository.hentMottatteDokumentMedFagsakId(fagsak.getId())
            .stream()
            .filter(md -> md.erSøknadsDokument())
            .filter(md -> md.getJournalpostId() != null)
            .filter(md -> md.getMottattTidspunkt() != null)
            .filter(md -> md.getBehandlingId() != null)
            .toList();
    }

    private Set<FpSak.Søknad> finnFpSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader, Fagsak fagsak) {
        return mottatteSøknader.stream()
            .map(md -> {
                var status = statusForSøknad(åpenYtelseBehandling, md.getBehandlingId());
                var perioder = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(md.getBehandlingId()).map(ytelseFordelingAggregat -> {
                    var oppgittFordeling = ytelseFordelingAggregat.getOppgittFordeling();
                    return oppgittFordeling.getPerioder().stream().map(FpOversiktDtoTjeneste::tilDto).collect(Collectors.toSet());
                }).orElse(Set.of());
                var oppgittDekingsgrad = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak).map(fr -> fr.getDekningsgrad());
                return new FpSak.Søknad(status, md.getMottattTidspunkt(), perioder, oppgittDekingsgrad.map(d -> tilDekningsgradDto(d)).orElse(null));
            })
            .filter(s -> !s.perioder().isEmpty()) //Filtrerer ut søknaden som ikke er registert i YF. Feks behandling står i papir punching
            .collect(Collectors.toSet());
    }

    private static FpSak.Dekningsgrad tilDekningsgradDto(Dekningsgrad dekningsgrad) {
        return dekningsgrad.isÅtti() ? FpSak.Dekningsgrad.ÅTTI : FpSak.Dekningsgrad.HUNDRE;
    }

    private static SøknadStatus statusForSøknad(Optional<Behandling> åpenYtelseBehandling, Long behandlingId) {
        return åpenYtelseBehandling.filter(b -> b.getId().equals(behandlingId)).map(b -> SøknadStatus.MOTTATT).orElse(SøknadStatus.BEHANDLET);
    }

    private static FpSak.Søknad.Periode tilDto(OppgittPeriodeEntitet periode) {
        var konto = switch (periode.getPeriodeType()) {
            case FELLESPERIODE -> Konto.FELLESPERIODE;
            case MØDREKVOTE -> Konto.MØDREKVOTE;
            case FEDREKVOTE -> Konto.FEDREKVOTE;
            case FORELDREPENGER -> Konto.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> Konto.FORELDREPENGER_FØR_FØDSEL;
            case ANNET, UDEFINERT -> null;
        };
        var utsettelseÅrsak = finnUtsettelseÅrsak(periode.getÅrsak());
        var oppholdÅrsak = finnOppholdÅrsak(periode.getÅrsak());
        var overføringÅrsak = finnOverføringÅrsak(periode.getÅrsak());
        var morsAktivitet = map(periode.getMorsAktivitet());
        var gradering = mapGradering(periode);
        var samtidigUttak = periode.getSamtidigUttaksprosent() == null ? null : periode.getSamtidigUttaksprosent().decimalValue();
        return new FpSak.Søknad.Periode(periode.getFom(), periode.getTom(), konto, utsettelseÅrsak, oppholdÅrsak, overføringÅrsak,
            gradering, samtidigUttak, periode.isFlerbarnsdager(), morsAktivitet);
    }

    private static MorsAktivitet map(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet morsAktivitet) {
        return morsAktivitet == null ? null : switch (morsAktivitet) {
            case UDEFINERT -> null;
            case ARBEID -> MorsAktivitet.ARBEID;
            case UTDANNING -> MorsAktivitet.UTDANNING;
            case KVALPROG -> MorsAktivitet.KVALPROG;
            case INTROPROG -> MorsAktivitet.INTROPROG;
            case TRENGER_HJELP -> MorsAktivitet.TRENGER_HJELP;
            case INNLAGT -> MorsAktivitet.INNLAGT;
            case ARBEID_OG_UTDANNING -> MorsAktivitet.ARBEID_OG_UTDANNING;
            case UFØRE -> MorsAktivitet.UFØRE;
            case IKKE_OPPGITT -> MorsAktivitet.IKKE_OPPGITT;
        };
    }

    private static Gradering mapGradering(OppgittPeriodeEntitet periode) {
        if (!erGradert(periode)) {
            return null;
        }
        var type = switch (periode.getGraderingAktivitetType()) {
            case ARBEID -> UttakAktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakAktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakAktivitet.Type.FRILANS;
        };
        var arbeidsgiver = periode.getArbeidsgiver() == null ? null : new Arbeidsgiver(periode.getArbeidsgiver().getIdentifikator());
        var aktivitet = new UttakAktivitet(type, arbeidsgiver, null);
        return new Gradering(periode.getArbeidsprosent(), aktivitet);
    }

    private static boolean erGradert(OppgittPeriodeEntitet periode) {
        return periode.isGradert() && periode.getGraderingAktivitetType() != null;
    }

    private static OverføringÅrsak finnOverføringÅrsak(Årsak årsak) {
        if (årsak instanceof no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak overføringÅrsak) {
            return switch (overføringÅrsak) {
                case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
                case SYKDOM_ANNEN_FORELDER -> OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
                case IKKE_RETT_ANNEN_FORELDER -> OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
                case ALENEOMSORG -> OverføringÅrsak.ALENEOMSORG;
                case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
            };
        }
        return null;
    }

    private static OppholdÅrsak finnOppholdÅrsak(Årsak årsak) {
        if (årsak instanceof no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak oppholdÅrsak) {
            return switch (oppholdÅrsak) {
                case MØDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
                case FEDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
                case KVOTE_FELLESPERIODE_ANNEN_FORELDER -> OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
                case KVOTE_FORELDREPENGER_ANNEN_FORELDER -> OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
                case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
            };
        }
        return null;
    }

    private static no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak finnUtsettelseÅrsak(Årsak årsak) {
        if (årsak instanceof UtsettelseÅrsak utsettelseÅrsak) {
            return switch (utsettelseÅrsak) {
                case ARBEID -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.ARBEID;
                case FERIE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.LOVBESTEMT_FERIE;
                case SYKDOM -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_SYKDOM;
                case INSTITUSJON_SØKER -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_INNLAGT;
                case INSTITUSJON_BARN -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.BARN_INNLAGT;
                case HV_OVELSE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.HV_ØVELSE;
                case NAV_TILTAK -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.NAV_TILTAK;
                case FRI -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.FRI;
                case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + årsak);
            };
        }
        return null;
    }

    private Set<SvpSak.Søknad> finnSvpSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream().map(md -> {
                var behandlingId = md.getBehandlingId();
                var status = statusForSøknad(åpenYtelseBehandling, behandlingId);
                var tilrettelegginger = finnTilrettelegginger(behandlingId);
                return new SvpSak.Søknad(status, md.getMottattTidspunkt(), tilrettelegginger);
            })
            .collect(Collectors.toSet());
    }

    private Set<SvpSak.Søknad.Tilrettelegging> finnTilrettelegginger(Long behandlingId) {
        if (isProd()) {
            return Set.of();
        }
        return svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .map(svpGrunnlag -> svpGrunnlag.getOpprinneligeTilrettelegginger().getTilretteleggingListe()
                .stream()
                .map(tl -> map(tl)).collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    private static boolean isProd() {
        return ENV.isProd();
    }

    private static SvpSak.Søknad.Tilrettelegging map(SvpTilretteleggingEntitet tl) {
        var aktivitet = utledAktivitet(tl);
        var perioder = tl.getTilretteleggingFOMListe().stream().map(tFom -> {
            SvpSak.TilretteleggingType tilretteleggingType = mapTilretteleggingType(tFom.getType());
            return new SvpSak.Søknad.Tilrettelegging.Periode(tFom.getFomDato(), tilretteleggingType, tFom.getStillingsprosent());
        }).collect(Collectors.toSet());
        Set<SvpSak.OppholdPeriode> oppholdsperioder = oppholdsperioderFraTilrettelegging(tl);
        return new SvpSak.Søknad.Tilrettelegging(aktivitet, tl.getBehovForTilretteleggingFom(), tl.getOpplysningerOmRisikofaktorer().orElse(null),
            tl.getOpplysningerOmTilretteleggingstiltak().orElse(null), perioder, oppholdsperioder);
    }

    private static SvpSak.TilretteleggingType mapTilretteleggingType(TilretteleggingType type) {
        return switch (type) {
            case HEL_TILRETTELEGGING -> SvpSak.TilretteleggingType.HEL;
            case DELVIS_TILRETTELEGGING -> SvpSak.TilretteleggingType.DELVIS;
            case INGEN_TILRETTELEGGING -> SvpSak.TilretteleggingType.INGEN;
        };
    }

    private static SvpSak.Aktivitet utledAktivitet(SvpTilretteleggingEntitet tl) {
        var aktivitetType = mapTilAktivitetType(tl.getArbeidType());
        var arbeidsgiver = tl.getArbeidsgiver().map(a -> new Arbeidsgiver(a.getIdentifikator())).orElse(null);
        return new SvpSak.Aktivitet(aktivitetType, arbeidsgiver, null);
    }

    private static SvpSak.Aktivitet.Type mapTilAktivitetType(ArbeidType arbeidType) {
        return switch (arbeidType) {
            case FRILANSER -> SvpSak.Aktivitet.Type.FRILANS;
            case ORDINÆRT_ARBEIDSFORHOLD -> SvpSak.Aktivitet.Type.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> SvpSak.Aktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
            default -> throw new IllegalStateException(UNEXPECTED_VALUE + arbeidType);
        };
    }

    private static Set<EsSak.Søknad> finnEsSøknader(Optional<Behandling> åpenYtelseBehandling, List<MottattDokument> mottatteSøknader) {
        return mottatteSøknader.stream()
            .map(md -> {
                var status = statusForSøknad(åpenYtelseBehandling, md.getBehandlingId());
                return new EsSak.Søknad(status, md.getMottattTidspunkt());
            })
            .collect(Collectors.toSet());
    }

    private List<Behandling> finnIkkeHenlagteBehandlinger(Fagsak fagsak) {
        return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId());
    }

    private Set<Sak.Aksjonspunkt> finnAksjonspunkt(List<Behandling> ikkeHenlagteBehandlinger) {
        return ikkeHenlagteBehandlinger.stream().flatMap(b -> b.getÅpneAksjonspunkter().stream())
            .map(a -> {
                var type = switch (a.getAksjonspunktDefinisjon()) {
                    case AUTO_MANUELT_SATT_PÅ_VENT -> Sak.Aksjonspunkt.Type.VENT_MANUELT_SATT;
                    case AUTO_VENT_PÅ_FØDSELREGISTRERING -> Sak.Aksjonspunkt.Type.VENT_FØDSEL;
                    case AUTO_VENTER_PÅ_KOMPLETT_SØKNAD -> Sak.Aksjonspunkt.Type.VENT_KOMPLETT_SØKNAD;
                    case AUTO_SATT_PÅ_VENT_REVURDERING -> Sak.Aksjonspunkt.Type.VENT_REVURDERING;
                    case VENT_PGA_FOR_TIDLIG_SØKNAD -> Sak.Aksjonspunkt.Type.VENT_TIDLIG_SØKNAD;
                    case AUTO_KØET_BEHANDLING -> Sak.Aksjonspunkt.Type.VENT_KØET_BEHANDLING;
                    case VENT_PÅ_SØKNAD -> Sak.Aksjonspunkt.Type.VENT_SØKNAD;
                    case AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST -> Sak.Aksjonspunkt.Type.VENT_INNTEKT_RAPPORTERINGSFRIST;
                    case AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT -> Sak.Aksjonspunkt.Type.VENT_SISTE_AAP_ELLER_DP_MELDEKORT;
                    case AUTO_VENT_ETTERLYST_INNTEKTSMELDING -> Sak.Aksjonspunkt.Type.VENT_ETTERLYST_INNTEKTSMELDING;
                    case AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN -> Sak.Aksjonspunkt.Type.VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN;
                    case AUTO_VENT_PÅ_SYKEMELDING -> Sak.Aksjonspunkt.Type.VENT_SYKEMELDING;
                    case AUTO_VENT_PÅ_KABAL_KLAGE -> Sak.Aksjonspunkt.Type.VENT_KABAL_KLAGE;
                    case AUTO_VENT_PÅ_KABAL_ANKE -> Sak.Aksjonspunkt.Type.VENT_PÅ_KABAL_ANKE;
                    default -> null;
                };

                var venteÅrsak = switch (a.getVenteårsak()) {
                    case ANKE_VENTER_PAA_MERKNADER_FRA_BRUKER -> Sak.Aksjonspunkt.Venteårsak.ANKE_VENTER_PÅ_MERKNADER_FRA_BRUKER;
                    case AVV_DOK -> Sak.Aksjonspunkt.Venteårsak.AVVENT_DOKUMTANSJON;
                    case AVV_FODSEL -> Sak.Aksjonspunkt.Venteårsak.AVVENT_FØDSEL;
                    case AVV_RESPONS_REVURDERING -> Sak.Aksjonspunkt.Venteårsak.AVVENT_RESPONS_REVURDERING;
                    case FOR_TIDLIG_SOKNAD -> Sak.Aksjonspunkt.Venteårsak.FOR_TIDLIG_SOKNAD;
                    case UTV_FRIST -> Sak.Aksjonspunkt.Venteårsak.UTVIDET_FRIST;
                    case VENT_INNTEKT_RAPPORTERINGSFRIST -> Sak.Aksjonspunkt.Venteårsak.INNTEKT_RAPPORTERINGSFRIST;
                    case VENT_MANGLENDE_SYKEMELDING -> Sak.Aksjonspunkt.Venteårsak.MANGLENDE_SYKEMELDING;
                    case VENT_OPDT_INNTEKTSMELDING -> Sak.Aksjonspunkt.Venteårsak.MANGLENDE_INNTEKTSMELDING;
                    case VENT_OPPTJENING_OPPLYSNINGER -> Sak.Aksjonspunkt.Venteårsak.OPPTJENING_OPPLYSNINGER;
                    case VENT_SØKNAD_SENDT_INFORMASJONSBREV -> Sak.Aksjonspunkt.Venteårsak.SENDT_INFORMASJONSBREV;
                    case VENT_ÅPEN_BEHANDLING -> Sak.Aksjonspunkt.Venteårsak.ÅPEN_BEHANDLING;
                    case VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT -> Sak.Aksjonspunkt.Venteårsak.SISTE_AAP_ELLER_DP_MELDEKORT;
                    default -> null;
                };
                return new Sak.Aksjonspunkt(type, venteÅrsak, a.getFristTid());
            })
            .filter(a -> a.type() != null)
            .collect(Collectors.toSet());
    }

    private boolean erAvsluttet(Fagsak fagsak) {
        return !fagsak.erÅpen();
    }

    private Sak.FamilieHendelse finnFamilieHendelse(Fagsak fagsak,
                                                    Optional<BehandlingVedtak> gjeldendeVedtak,
                                                    Optional<Behandling> åpenYtelseBehandling) {
        if (gjeldendeVedtak.isPresent()) {
            return finnGjeldendeFamilieHendelse(gjeldendeVedtak.get().getBehandlingsresultat().getBehandlingId());
        }
        if (åpenYtelseBehandling.isPresent()) {
            return finnBekreftetSøknadFamilieHendelse(åpenYtelseBehandling.get().getId()).orElse(null);
        }
        var sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        return sisteBehandling.flatMap(b -> finnBekreftetSøknadFamilieHendelse(b.getId())).orElse(null);
    }

    private Optional<Behandling> hentÅpenBehandling(Fagsak fagsak) {
        var åpenYtelseBehandling = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId())
            .stream()
            .filter(b -> b.erYtelseBehandling())
            .max(Comparator.comparing(Behandling::getOpprettetTidspunkt));
        åpenYtelseBehandling.ifPresentOrElse(b -> {
            LOG.info("Fant åpen ytelsebehandling for sak {} {}", fagsak.getSaksnummer(), b.getId());
        }, () -> LOG.info("Ingen åpen ytelsebehandling for sak {}", fagsak.getSaksnummer()));
        return åpenYtelseBehandling;
    }

    private Optional<Sak.FamilieHendelse> finnBekreftetSøknadFamilieHendelse(Long behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling).map(agg -> {
            var versjon = agg.getBekreftetVersjon().orElseGet(agg::getSøknadVersjon);
            return tilDto(versjon);
        });
    }

    private Sak.FamilieHendelse finnGjeldendeFamilieHendelse(Long behandling) {
        return familieHendelseTjeneste.finnAggregat(behandling)
            .map(fhg -> tilDto(fhg.getGjeldendeVersjon()))
            .orElse(null);
    }

    private static Sak.FamilieHendelse tilDto(FamilieHendelseEntitet familieHendelse) {
        return new Sak.FamilieHendelse(familieHendelse.getFødselsdato().orElse(null),
            familieHendelse.getTerminbekreftelse().map(tb -> tb.getTermindato()).orElse(null),
            familieHendelse.getAntallBarn() == null ? 0 : familieHendelse.getAntallBarn(),
            familieHendelse.getAdopsjon().map(a -> a.getOmsorgsovertakelseDato()).orElse(null));
    }

    private Optional<AktørId> oppgittAnnenPart(Fagsak fagsak) {
        var førstegangsbehandling = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(),
                BehandlingType.FØRSTEGANGSSØKNAD)
            .orElseGet(() -> behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId())
                .stream()
                .max(Comparator.comparing(Behandling::getOpprettetDato))
                .orElseThrow());
        return personopplysningTjeneste.hentOppgittAnnenPartAktørId(førstegangsbehandling.getId());
    }

    private Set<FpSak.Vedtak> finnFpVedtak(Fagsak fagsak, Stream<BehandlingVedtak> vedtak) {
        return vedtak.map(v -> tilDto(v, fagsak)).collect(Collectors.toSet());
    }

    private Stream<BehandlingVedtak> finnVedtakForFagsak(Fagsak fagsak) {
        var behandlingerMedVedtak = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsak.getId());
        return behandlingerMedVedtak.stream()
            .map(b -> vedtakRepository.hentForBehandlingHvisEksisterer(b.getId()))
            .filter(Optional::isPresent)
            .map(v -> v.get());
    }

    private FpSak.Vedtak tilDto(BehandlingVedtak vedtak, Fagsak fagsak) {
        var uttaksperioder = finnUttaksperioder(vedtak.getBehandlingsresultat().getBehandlingId());
        var dekningsgrad = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak).map(fr -> fr.getGjeldendeDekningsgrad());
        return new FpSak.Vedtak(vedtak.getVedtakstidspunkt(), uttaksperioder, dekningsgrad.map(d -> tilDekningsgradDto(d)).orElse(null));
    }

    private List<FpSak.Uttaksperiode> finnUttaksperioder(Long behandlingId) {
        return foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandlingId, true).map(uttak -> tilDto(uttak.getGjeldendePerioder())).orElse(List.of());
    }

    private List<FpSak.Uttaksperiode> tilDto(List<ForeldrepengerUttakPeriode> gjeldendePerioder) {
        return gjeldendePerioder.stream().map(this::tilDto).toList();
    }

    private FpSak.Uttaksperiode tilDto(ForeldrepengerUttakPeriode periode) {
        var type = utledResultatType(periode);
        var aktiviteter = periode.getAktiviteter().stream().map(a -> {
            var aktivitetType = switch (a.getUttakArbeidType()) {
                case ORDINÆRT_ARBEID -> UttakAktivitet.Type.ORDINÆRT_ARBEID;
                case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakAktivitet.Type.SELVSTENDIG_NÆRINGSDRIVENDE;
                case FRILANS -> UttakAktivitet.Type.FRILANS;
                case ANNET -> UttakAktivitet.Type.ANNET;
            };
            var arbeidsgiver = a.getArbeidsgiver().map(arb -> new Arbeidsgiver(arb.getIdentifikator())).orElse(null);
            var arbeidsforholdId = Optional.ofNullable(a.getArbeidsforholdRef()).map(InternArbeidsforholdRef::getReferanse).orElse(null);
            var trekkdager = a.getTrekkdager().decimalValue();
            var konto = switch (a.getTrekkonto()) {
                case FELLESPERIODE -> Konto.FELLESPERIODE;
                case MØDREKVOTE -> Konto.MØDREKVOTE;
                case FEDREKVOTE -> Konto.FEDREKVOTE;
                case FORELDREPENGER -> Konto.FORELDREPENGER;
                case FORELDREPENGER_FØR_FØDSEL -> Konto.FORELDREPENGER_FØR_FØDSEL;
                case UDEFINERT, FLERBARNSDAGER -> null;
            };
            var arbeidstidsprosent = a.isSøktGraderingForAktivitetIPeriode() ? a.getArbeidsprosent() : BigDecimal.ZERO;
            return new FpSak.Uttaksperiode.UttaksperiodeAktivitet(new UttakAktivitet(aktivitetType, arbeidsgiver, arbeidsforholdId), konto,
                trekkdager, arbeidstidsprosent);
        }).collect(Collectors.toSet());
        var årsak = switch (periode.getResultatÅrsak()) {
            case HULL_MELLOM_FORELDRENES_PERIODER, BARE_FAR_RETT_IKKE_SØKT -> Resultat.Årsak.AVSLAG_HULL_I_UTTAKSPLAN;
            default -> Resultat.Årsak.ANNET;
        };
        var trekkerMinsterett = trekkerMinsterett(periode);
        var resultat = new Resultat(type, årsak, aktiviteter, trekkerMinsterett);
        var utsettelseÅrsak = switch (periode.getUtsettelseType()) {
            case ARBEID -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.ARBEID;
            case FERIE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.LOVBESTEMT_FERIE;
            case SYKDOM_SKADE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_SYKDOM;
            case SØKER_INNLAGT -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.SØKER_INNLAGT;
            case BARN_INNLAGT -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.BARN_INNLAGT;
            case HV_OVELSE -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.HV_ØVELSE;
            case NAV_TILTAK -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.NAV_TILTAK;
            case FRI -> no.nav.foreldrepenger.web.app.tjenester.fpoversikt.UtsettelseÅrsak.FRI;
            case UDEFINERT -> null;
        };
        var oppholdÅrsak = switch (periode.getOppholdÅrsak()) {
            case UDEFINERT -> null;
            case MØDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
            case FEDREKVOTE_ANNEN_FORELDER -> OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
            case KVOTE_FELLESPERIODE_ANNEN_FORELDER -> OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
            case KVOTE_FORELDREPENGER_ANNEN_FORELDER -> OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
        };
        var overføringÅrsak = periode.isSøktOverføring() ? switch (periode.getOverføringÅrsak()) {
            case INSTITUSJONSOPPHOLD_ANNEN_FORELDER -> OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER;
            case SYKDOM_ANNEN_FORELDER -> OverføringÅrsak.SYKDOM_ANNEN_FORELDER;
            case IKKE_RETT_ANNEN_FORELDER -> OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER;
            case ALENEOMSORG -> OverføringÅrsak.ALENEOMSORG;
            case UDEFINERT -> null;
        } : null;
        var samtidigUttaksprosent = periode.getSamtidigUttaksprosent();
        return new FpSak.Uttaksperiode(periode.getFom(), periode.getTom(), utsettelseÅrsak, oppholdÅrsak, overføringÅrsak,
            samtidigUttaksprosent == null ? null : samtidigUttaksprosent.decimalValue(), periode.isFlerbarnsdager(),
            map(periode.getMorsAktivitet()), resultat);
    }

    private boolean trekkerMinsterett(ForeldrepengerUttakPeriode periode) {
        return periode.harTrekkdager() && !Set.of(PeriodeResultatÅrsak.FORELDREPENGER_KUN_FAR_HAR_RETT,
            PeriodeResultatÅrsak.GRADERING_FORELDREPENGER_KUN_FAR_HAR_RETT).contains(periode.getResultatÅrsak());
    }

    private static Resultat.Type utledResultatType(ForeldrepengerUttakPeriode periode) {
        if (periode.isInnvilget() && periode.isGraderingInnvilget()) {
            return Resultat.Type.INNVILGET_GRADERING;
        }
        return switch (periode.getResultatType()) {
            case INNVILGET -> Resultat.Type.INNVILGET;
            case AVSLÅTT -> Resultat.Type.AVSLÅTT;
            case MANUELL_BEHANDLING -> throw new IllegalStateException("Forventer ikke perioder under manuell behandling");
        };
    }
}
