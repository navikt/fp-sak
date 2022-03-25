package no.nav.foreldrepenger.mottak.vedtak.kafka;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.HåndterOverlappPleiepengerTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.log.util.LoggerUtils;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);

    private static final Map<YtelseType, FagsakYtelseType> YTELSE_TYPE_MAP = Map.of(
            YtelseType.ENGANGSTØNAD, FagsakYtelseType.ENGANGSTØNAD,
            YtelseType.FORELDREPENGER, FagsakYtelseType.FORELDREPENGER,
            YtelseType.SVANGERSKAPSPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final Map<Ytelser, FagsakYtelseType> YTELSER_MAP = Map.of(
        Ytelser.ENGANGSTØNAD, FagsakYtelseType.ENGANGSTØNAD,
        Ytelser.FORELDREPENGER, FagsakYtelseType.FORELDREPENGER,
        Ytelser.SVANGERSKAPSPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final boolean isProd = Environment.current().isProd();

    private FagsakTjeneste fagsakTjeneste;
    private LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private ProsessTaskTjeneste taskTjeneste;

    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(FagsakTjeneste fagsakTjeneste, BeregningsresultatRepository tilkjentYtelseRepository,
            BehandlingRepository behandlingRepository,
            LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger,
                                    ProsessTaskTjeneste taskTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.eksternOverlappLogger = eksternOverlappLogger;
        this.behandlingRepository = behandlingRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.taskTjeneste = taskTjeneste;
    }

    void handleMessage(String key, String payload) {
        // enhver exception ut fra denne metoden medfører at tråden som leser fra kafka
        // gir opp og dør på seg.
        try {
            var mottattVedtak = StandardJsonConfig.fromJson(payload, Ytelse.class);
            handleMessageIntern(mottattVedtak);
        } catch (VLException e) {
            LOG.warn("FP-328773 Vedtatt-Ytelse Feil under parsing av vedtak. key={} payload={}", key, payload, e);
        } catch (Exception e) {
            LOG.warn("Vedtatt-Ytelse exception ved håndtering av vedtaksmelding, ignorerer key={}", LoggerUtils.removeLineBreaks(payload), e);
        }
    }

    void handleMessageIntern(Ytelse mottattVedtak) {
        if (mottattVedtak == null)
            return;
        var ytelse = (YtelseV1) mottattVedtak;

        if (Fagsystem.FPSAK.equals(ytelse.getFagsystem()) || (ytelse.getKildesystem() != null && Kildesystem.FPSAK.equals(ytelse.getKildesystem()))) {
            oprettTasksForFpsakVedtak(ytelse);
        } else if (skalLoggeOverlappDB(ytelse)) {
            var fagsaker = getFagsakerFor(ytelse);
            loggVedtakOverlapp(ytelse, fagsaker);
        } else if (YtelseType.PLEIEPENGER_SYKT_BARN.equals(ytelse.getType()) || (ytelse.getYtelse() != null && Ytelser.PLEIEPENGER_SYKT_BARN.equals(ytelse.getYtelse()))) {
            var fagsaker = getFagsakerFor(ytelse);
            var callID = UUID.randomUUID();
            fagsakerMedVedtakOverlapp(ytelse, fagsaker)
                .forEach(f -> opprettHåndterOverlappTaskPleiepenger(f, callID));
        } else {
            var system = Optional.ofNullable(ytelse.getKildesystem()).map(Kildesystem::name)
                .orElseGet(() -> Optional.ofNullable(ytelse.getFagsystem()).map(Fagsystem::getKode).orElse(null));
            var ytelseTypeTekst = Optional.ofNullable(ytelse.getYtelse()).map(Ytelser::name)
                .orElseGet(() -> Optional.ofNullable(ytelse.getType()).map(YtelseType::getKode).orElse(null));
            LOG.info("Vedtatt-Ytelse mottok vedtak fra system {} saksnummer {} ytelse {}", system,
                ytelse.getSaksnummer(), ytelseTypeTekst);
            var fagsaker = getFagsakerFor(ytelse);
            LOG.info("Vedtatt-Ytelse VL har disse sakene for bruker med vedtak {} - saker {}",
                ytelseTypeTekst, fagsaker.stream().map(Fagsak::getSaksnummer).collect(Collectors.toList()));

            var fagsakerMedOverlapp = fagsakerMedVedtakOverlapp(ytelse, fagsaker);
            if (!fagsakerMedOverlapp.isEmpty()) {
                var overlappSaksnummerList =
                    fagsakerMedOverlapp.stream().map(Fagsak::getSaksnummer).map(Saksnummer::getVerdi).collect(Collectors.toList());
                var beskrivelse = String.format("Vedtak om %s sak %s overlapper saker i VL: %s",
                    ytelseTypeTekst, ytelse.getSaksnummer(), String.join(", ", overlappSaksnummerList));
                LOG.warn("Vedtatt-Ytelse KONTAKT PRODUKTEIER UMIDDELBART! - {}", beskrivelse);
                loggVedtakOverlapp(ytelse, fagsakerMedOverlapp);
            }
        }
    }

    private boolean skalLoggeOverlappDB(YtelseV1 ytelse) {
        return Set.of(YtelseType.FRISINN, YtelseType.OMSORGSPENGER).contains(ytelse.getType())
            || (ytelse.getYtelse() != null && Set.of(Ytelser.FRISINN, Ytelser.OMSORGSPENGER).contains(ytelse.getYtelse()));
    }

    private void oprettTasksForFpsakVedtak(YtelseV1 ytelse) {
        var fagsakYtelseType = Optional.ofNullable(ytelse.getYtelse()).map(YTELSER_MAP::get)
            .orElseGet(() -> Optional.ofNullable(ytelse.getType()).map(YTELSE_TYPE_MAP::get).orElse(FagsakYtelseType.UDEFINERT));

        if (!VURDER_OVERLAPP.contains(fagsakYtelseType)) {
            if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
                var ytelseTypeTekst = Optional.ofNullable(ytelse.getYtelse()).map(Ytelser::name)
                    .orElseGet(() -> Optional.ofNullable(ytelse.getType()).map(YtelseType::getKode).orElse(null));

                LOG.error("Vedtatt-Ytelse Utviklerfeil: ukjent ytelsestype for innkommende vedtak {}", ytelseTypeTekst);
            }
            return;
        }

        try {
            var behandlingUuid = UUID.fromString(ytelse.getVedtakReferanse());
            var behandling = behandlingRepository.hentBehandling(behandlingUuid);

            // Unngå gå i beina på på iverksettingstasker med sen respons
            if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
                if (isProd) {
                    var startberørtdelay = 1 + LocalDateTime.now().getNano() % 3;
                    lagreProsesstaskFor(behandling, TaskType.forProsessTask(StartBerørtBehandlingTask.class), startberørtdelay);
                    lagreProsesstaskFor(behandling, TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), 5);
                } else {
                    lagreProsesstaskFor(behandling, TaskType.forProsessTask(StartBerørtBehandlingTask.class), 0);
                    lagreProsesstaskFor(behandling, TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), 2);
                }
            } else { // SVP
                lagreProsesstaskFor(behandling, TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), 0);
            }
        } catch (Exception e) {
            LOG.error("Vedtatt-Ytelse mottok vedtak med ugyldig behandling-UUID som ikke finnes i database");
        }
    }

    private void opprettHåndterOverlappTaskPleiepenger(Fagsak f, UUID callID) {
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterOverlappPleiepengerTask.class);
        prosessTaskData.setFagsak(f.getId(), f.getAktørId().getId());
        // Gi abakus tid til å konsumere samme hendelse så det finnes et grunnlag å hente opp.
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusMinutes(10));
        prosessTaskData.setCallId(callID.toString());
        taskTjeneste.lagre(prosessTaskData);
    }

    // Flytt flere eksterne ytelser hit når de er etablert. Utbetalinggrad null = 100.
    void loggVedtakOverlapp(YtelseV1 ytelse, List<Fagsak> fagsaker) {
        eksternOverlappLogger.loggOverlappForVedtakK9SAK(ytelse, fagsaker);
    }

    // Kommende ytelser - gi varsel i applikasjonslogg før databaselogging
    boolean sjekkVedtakOverlapp(YtelseV1 ytelse, List<Fagsak> fagsaker) {
        return !fagsakerMedVedtakOverlapp(ytelse, fagsaker).isEmpty();
    }

    private List<Fagsak> fagsakerMedVedtakOverlapp(YtelseV1 ytelse, List<Fagsak> fagsaker) {
        // OBS Flere av K9SAK-ytelsene har fom/tom i helg ... ikke bruk VirkedagUtil på dem.
        var ytelsesegments = ytelse.getAnvist().stream()
                // .filter(p -> p.utbetalingsgrad().getVerdi().compareTo(BigDecimal.ZERO) >  0)
                .map(p -> new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), Boolean.TRUE))
                .collect(Collectors.toList());
        if (ytelsesegments.isEmpty() || fagsaker.isEmpty())
            return List.of();

        var minYtelseDato = ytelsesegments.stream().map(LocalDateSegment::getFom).min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
        var ytelseTidslinje = new LocalDateTimeline<>(ytelsesegments, StandardCombinators::alwaysTrueForMatch).compress();

        var behandlinger = fagsaker.stream()
                .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
                .filter(b -> sjekkOverlappFor(minYtelseDato, ytelseTidslinje, b))
                .collect(Collectors.toList());

        return behandlinger.stream().map(Behandling::getFagsak).collect(Collectors.toList());
    }

    private List<Fagsak> getFagsakerFor(YtelseV1 ytelse) {
        return fagsakTjeneste.finnFagsakerForAktør(new AktørId(ytelse.getAktør().getVerdi())).stream()
                .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
                .collect(Collectors.toList());
    }

    private boolean sjekkOverlappFor(LocalDate minYtelseDato, LocalDateTimeline<Boolean> ytelseTidslinje, Behandling behandling) {
        var fpsegments = tilkjentYtelseRepository.hentUtbetBeregningsresultat(behandling.getId())
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                .filter(p -> p.getDagsats() > 0)
                .filter(p -> p.getBeregningsresultatPeriodeTom().isAfter(minYtelseDato.minusDays(1)))
                .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(), Boolean.TRUE))
                .collect(Collectors.toList());

        if (fpsegments.isEmpty())
            return false;

        var fpTidslinje = new LocalDateTimeline<>(fpsegments, StandardCombinators::alwaysTrueForMatch).compress();
        return !fpTidslinje.intersection(ytelseTidslinje).getLocalDateIntervals().isEmpty();
    }

    void lagreProsesstaskFor(Behandling behandling, TaskType taskType, int delaysecs) {
        var data = ProsessTaskData.forTaskType(taskType);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallId(behandling.getUuid().toString());
        data.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(delaysecs));
        taskTjeneste.lagre(data);
    }
}
