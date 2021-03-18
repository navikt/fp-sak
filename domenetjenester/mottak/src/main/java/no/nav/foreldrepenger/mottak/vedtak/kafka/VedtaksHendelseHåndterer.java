package no.nav.foreldrepenger.mottak.vedtak.kafka;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.json.JacksonJsonConfig;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.log.util.LoggerUtils;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);
    private static final ObjectMapper OBJECT_MAPPER = JacksonJsonConfig.getMapper();

    private static final Map<YtelseType, FagsakYtelseType> YTELSE_TYPE_MAP = Map.of(
            YtelseType.ENGANGSTØNAD, FagsakYtelseType.ENGANGSTØNAD,
            YtelseType.FORELDREPENGER, FagsakYtelseType.FORELDREPENGER,
            YtelseType.SVANGERSKAPSPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final Set<YtelseType> DB_LOGGES = Set.of(YtelseType.FRISINN, YtelseType.OMSORGSPENGER);
    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);

    private FagsakTjeneste fagsakTjeneste;
    private LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private Validator validator;
    private ProsessTaskRepository prosessTaskRepository;

    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(FagsakTjeneste fagsakTjeneste, BeregningsresultatRepository tilkjentYtelseRepository,
            BehandlingRepository behandlingRepository,
            LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger,
            ProsessTaskRepository prosessTaskRepository) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.eksternOverlappLogger = eksternOverlappLogger;
        this.behandlingRepository = behandlingRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        @SuppressWarnings("resource")
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    void handleMessage(String key, String payload) {
        // enhver exception ut fra denne metoden medfører at tråden som leser fra kafka
        // gir opp og dør på seg.
        try {
            var mottattVedtak = OBJECT_MAPPER.readValue(payload, Ytelse.class);
            Set<ConstraintViolation<Ytelse>> violations = validator.validate(mottattVedtak);
            if (!violations.isEmpty()) {
                // Har feilet validering
                String allErrors = violations.stream().map(String::valueOf).collect(Collectors.joining("\\n"));
                LOG.warn("Vedtatt-Ytelse valideringsfeil :: \n {}", allErrors);
                return;
            }
            handleMessageIntern(mottattVedtak);
        } catch (IOException e) {
            LOG.warn("FP-328773 Vedtatt-Ytelse Feil under parsing av vedtak. key={} payload={}", key, payload, e);
        } catch (Exception e) {
            LOG.warn("Vedtatt-Ytelse exception ved håndtering av vedtaksmelding, ignorerer key={}", LoggerUtils.removeLineBreaks(payload), e);
        }
    }

    void handleMessageIntern(Ytelse mottattVedtak) {
        if (mottattVedtak == null)
            return;
        var ytelse = (YtelseV1) mottattVedtak;

        if (Fagsystem.FPSAK.equals(ytelse.getFagsystem())) {
            oprettTasksForFpsakVedtak(ytelse);
        } else if (DB_LOGGES.contains(ytelse.getType())) {
            loggVedtakOverlapp(ytelse);
        } else {
            LOG.info("Vedtatt-Ytelse mottok vedtak fra system {} saksnummer {} ytelse {}", ytelse.getFagsystem(), ytelse.getSaksnummer(),
                    ytelse.getType());
            sjekkVedtakOverlapp(ytelse);
        }
    }

    void oprettTasksForFpsakVedtak(YtelseV1 ytelse) {
        FagsakYtelseType fagsakYtelseType = YTELSE_TYPE_MAP.getOrDefault(ytelse.getType(), FagsakYtelseType.UDEFINERT);

        if (!VURDER_OVERLAPP.contains(fagsakYtelseType)) {
            if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
                LOG.error("Vedtatt-Ytelse Utviklerfeil: ukjent ytelsestype for innkommende vedtak {}", ytelse.getType());
            }
            return;
        }

        try {
            UUID behandlingUuid = UUID.fromString(ytelse.getVedtakReferanse());
            Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);

            // Unngå gå i beina på på iverksettingstasker med sen respons
            if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
                lagreProsesstaskFor(behandling, StartBerørtBehandlingTask.TASKTYPE, 0);
                lagreProsesstaskFor(behandling, VurderOpphørAvYtelserTask.TASKTYPE, 2);
            } else { // SVP
                lagreProsesstaskFor(behandling, VurderOpphørAvYtelserTask.TASKTYPE, 0);
            }
        } catch (Exception e) {
            LOG.error("Vedtatt-Ytelse mottok vedtak med ugyldig behandling-UUID som ikke finnes i database");
        }
    }

    // Flytt flere eksterne ytelser hit når de er etablert. Utbetalinggrad null =
    // 100.
    void loggVedtakOverlapp(YtelseV1 ytelse) {
        List<Fagsak> fagsaker = getFagsakerFor(ytelse);

        eksternOverlappLogger.loggOverlappForVedtakK9SAK(ytelse, fagsaker);
    }

    // Kommende ytelser - gi varsel i applikasjonslogg før databaselogging
    boolean sjekkVedtakOverlapp(YtelseV1 ytelse) {
        List<Fagsak> fagsaker = getFagsakerFor(ytelse);

        // OBS Flere av K9SAK-ytelsene har fom/tom i helg ... ikke bruk VirkedagUtil på
        // dem.
        List<LocalDateSegment<Boolean>> ytelsesegments = ytelse.getAnvist().stream()
                // .filter(p -> p.getUtbetalingsgrad().getVerdi().compareTo(BigDecimal.ZERO) >
                // 0)
                .map(p -> new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), Boolean.TRUE))
                .collect(Collectors.toList());
        if (ytelsesegments.isEmpty() || fagsaker.isEmpty())
            return false;

        LOG.info("Vedtatt-Ytelse VL har disse sakene for bruker med vedtak {} - saker {}", ytelse.getType(),
                fagsaker.stream().map(Fagsak::getSaksnummer).collect(Collectors.toList()));

        var minYtelseDato = ytelsesegments.stream().map(LocalDateSegment::getFom).min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
        var ytelseTidslinje = new LocalDateTimeline<>(ytelsesegments, StandardCombinators::alwaysTrueForMatch).compress();

        List<Behandling> behandlinger = fagsaker.stream()
                .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
                .filter(b -> sjekkOverlappFor(minYtelseDato, ytelseTidslinje, b))
                .sorted(Comparator.comparing(Behandling::getOpprettetDato).reversed())
                .collect(Collectors.toList());

        if (!behandlinger.isEmpty()) {
            var overlappsaker = behandlinger.stream().map(Behandling::getFagsak).map(Fagsak::getSaksnummer).map(Saksnummer::getVerdi)
                    .collect(Collectors.joining(", "));
            var beskrivelse = String.format("Vedtak om %s sak %s overlapper saker i VL: %s", ytelse.getType().getNavn(), ytelse.getSaksnummer(),
                    overlappsaker);
            LOG.warn("Vedtatt-Ytelse KONTAKT PRODUKTEIER UMIDDELBART! - {}", beskrivelse);
            // TODO (jol): enable VKY etter avklaring. Deretter vurder å opprette
            // revurdering .... Behovet tilstede for PSB, øvrige uklare
            // vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(behandlinger.get(0).getFagsakId(),
            // behandlinger.get(0).getBehandlendeEnhet(),
            // beskrivelse, Optional.of(ytelse.getAktør().getVerdi()));
        }
        return !behandlinger.isEmpty();
    }

    private List<Fagsak> getFagsakerFor(YtelseV1 ytelse) {
        return fagsakTjeneste.finnFagsakerForAktør(new AktørId(ytelse.getAktør().getVerdi())).stream()
                .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
                .collect(Collectors.toList());
    }

    private boolean sjekkOverlappFor(LocalDate minYtelseDato, LocalDateTimeline<Boolean> ytelseTidslinje, Behandling behandling) {
        List<LocalDateSegment<Boolean>> fpsegments = tilkjentYtelseRepository.hentUtbetBeregningsresultat(behandling.getId())
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

    void lagreProsesstaskFor(Behandling behandling, String taskType, int delaysecs) {
        ProsessTaskData data = new ProsessTaskData(taskType);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallIdFraEksisterende();
        data.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(delaysecs));
        prosessTaskRepository.lagre(data);
    }
}
