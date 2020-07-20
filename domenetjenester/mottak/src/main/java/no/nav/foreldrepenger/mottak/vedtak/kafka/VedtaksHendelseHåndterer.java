package no.nav.foreldrepenger.mottak.vedtak.kafka;

import java.io.IOException;
import java.math.BigDecimal;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.json.JacksonJsonConfig;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappendeEksternYtelseTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
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
        YtelseType.SVANGERSKAPSPENGER, FagsakYtelseType.SVANGERSKAPSPENGER
    );
    private static final Set<YtelseType> DB_LOGGES = Set.of(YtelseType.FRISINN, YtelseType.OMSORGSPENGER);

    private FagsakTjeneste fagsakTjeneste;
    private LoggOverlappendeEksternYtelseTjeneste eksternOverlappLogger;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private Validator validator;
    private ProsessTaskRepository prosessTaskRepository;


    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(FagsakTjeneste fagsakTjeneste,
                                    LoggOverlappendeEksternYtelseTjeneste eksternOverlappLogger,
                                    BehandlingRepositoryProvider repositoryProvider,
                                    ProsessTaskRepository prosessTaskRepository) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.eksternOverlappLogger = eksternOverlappLogger;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilkjentYtelseRepository = repositoryProvider.getBeregningsresultatRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        @SuppressWarnings("resource")
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        // hibernate validator implementations er thread-safe, trenger ikke close
        validator = factory.getValidator();
    }

    void handleMessage(String key, String payload) {
        //enhver exception ut fra denne metoden medfører at tråden som leser fra kafka gir opp og dør på seg.
        //transitive feil bør egentlig medføre retry, meldinger som ikke validerer bør ignoreres
        try {
            handleMessageIntern(key, payload);
        } catch (Exception e) {
            LOG.warn("Oppstod exception ved håndtering av vedtaksmelding key=" + LoggerUtils.removeLineBreaks(key) + ". Meldingen ble ignorert", e);
        }
    }

    void handleMessageIntern(String key, String payload) {
        Ytelse mottattVedtak;
        try {
            mottattVedtak = OBJECT_MAPPER.readValue(payload, Ytelse.class);
            Set<ConstraintViolation<Ytelse>> violations = validator.validate(mottattVedtak);
            if (!violations.isEmpty()) {
                // Har feilet validering
                String allErrors = violations.stream().map(String::valueOf).collect(Collectors.joining("\\n"));
                LOG.info("Vedtatt-Ytelse valideringsfeil :: \n {}", allErrors);
                return;
            }
        } catch (IOException e) {
            YtelseFeil.FACTORY.parsingFeil(key, payload, e).log(LOG);
            return;
        }
        if (mottattVedtak == null)
            return;
        var ytelse = (YtelseV1) mottattVedtak;

        if (Fagsystem.FPSAK.equals(ytelse.getFagsystem())) {
            oprettTasksForFpsakVedtak(ytelse);
        } else {
            LOG.info("Vedtatt-Ytelse mottok vedtak fra system {} saksnummer {} ytelse {}", ytelse.getFagsystem(), ytelse.getSaksnummer(), ytelse.getType());
            sjekkVedtakOverlapp(ytelse);
        }
    }

    void oprettTasksForFpsakVedtak(YtelseV1 ytelse) {
        FagsakYtelseType fagsakYtelseType = YTELSE_TYPE_MAP.getOrDefault(ytelse.getType(), FagsakYtelseType.UDEFINERT);

        if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
            LOG.error("Utviklerfeil: ukjent ytelsestype for innkommende vedtak {}", ytelse.getType());
            return;
        }

        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType)) {
            return;
        }

        Behandling behandling;
        try {
            UUID behandlingUuid = UUID.fromString(ytelse.getVedtakReferanse());
            behandling = behandlingRepository.hentBehandling(behandlingUuid);
        } catch (Exception e) {
            return;
        }
        // Unngå gå i beina på på iverksettingstasker med sen respons
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            lagreProsesstaskFor(behandling, StartBerørtBehandlingTask.TASKTYPE, 10);
            lagreProsesstaskFor(behandling, VurderOpphørAvYtelserTask.TASKTYPE, 15);
        } else { //SVP
            lagreProsesstaskFor(behandling, VurderOpphørAvYtelserTask.TASKTYPE, 10);
        }
    }

    void sjekkVedtakOverlapp(YtelseV1 ytelse) {
        // OBS Flere av K9SAK-ytelsene har fom/tom i helg ... ikke bruk VirkedagUtil på dem
        var harSattUtbetalingsgrad = ytelse.getAnvist().stream().anyMatch(a -> a.getUtbetalingsgrad() != null && a.getUtbetalingsgrad().getVerdi() != null);
        List<LocalDateSegment<Boolean>> ytelsesegments = ytelse.getAnvist().stream()
            // .filter(p -> p.getUtbetalingsgrad().getVerdi().compareTo(BigDecimal.ZERO) > 0)
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), Boolean.TRUE))
            .collect(Collectors.toList());
        if (ytelsesegments.isEmpty())
            return;

        List<Fagsak> fagsaker = fagsakTjeneste.finnFagsakerForAktør(new AktørId(ytelse.getAktør().getVerdi())).stream()
            .filter(f -> !FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .collect(Collectors.toList());
        if (fagsaker.isEmpty())
            return;
        LOG.info("Vedtatt-Ytelse VL har disse sakene for bruker med vedtak {} - saker {}", ytelse.getType(), fagsaker.stream().map(Fagsak::getSaksnummer).collect(Collectors.toList()));

        var minYtelseDato = ytelsesegments.stream().map(LocalDateSegment::getFom).min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
        var ytelseTidslinje = new LocalDateTimeline<>(ytelsesegments, StandardCombinators::alwaysTrueForMatch).compress();

        // Flytt flere ytelser hit. Frisinn må logges ugradert. Vurder egen metode for de som kan sjekkes mot gradert overlapp - bruk da LDTL / BigDecmial
        if (DB_LOGGES.contains(ytelse.getType())) {
            if (!harSattUtbetalingsgrad) {
                eksternOverlappLogger.loggOverlappUtenGradering(ytelse, minYtelseDato, ytelseTidslinje, fagsaker);
                return;
            } else {
                List<LocalDateSegment<BigDecimal>> graderteSegments = ytelse.getAnvist().stream()
                    .map(p -> new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), p.getUtbetalingsgrad().getVerdi()))
                    .collect(Collectors.toList());
                var gradertTidslinje = new LocalDateTimeline<>(graderteSegments, StandardCombinators::sum).filterValue(v -> v.compareTo(BigDecimal.ZERO) > 0);

                eksternOverlappLogger.loggOverlappMedGradering(ytelse, minYtelseDato, gradertTidslinje, fagsaker);
                return;
            }
        }

        List<Behandling> behandlinger = fagsaker.stream()
            .map(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()))
            .flatMap(Optional::stream)
            .filter(b -> sjekkOverlappFor(minYtelseDato, ytelseTidslinje, b))
            .sorted(Comparator.comparing(Behandling::getOpprettetDato).reversed())
            .collect(Collectors.toList());

        if (!behandlinger.isEmpty()) {
            var overlappsaker = behandlinger.stream().map(Behandling::getFagsak).map(Fagsak::getSaksnummer).map(Saksnummer::getVerdi).collect(Collectors.joining(", "));
            var beskrivelse = String.format("Vedtak om %s sak %s overlapper saker i VL: %s", ytelse.getType().getNavn(), ytelse.getSaksnummer(), overlappsaker);
            LOG.warn("Vedtatt-Ytelse KONTAKT PRODUKTEIER UMIDDELBART! - {}", beskrivelse);
            // TODO (jol): enable VKY etter avklaring. Deretter vurder å opprette revurdering .... Behovet tilstede for PSB, øvrige uklare
            // vurderOpphørAvYtelser.opprettTaskForÅVurdereKonsekvens(behandlinger.get(0).getFagsakId(), behandlinger.get(0).getBehandlendeEnhet(),
            //    beskrivelse, Optional.of(ytelse.getAktør().getVerdi()));
        }
    }

    private boolean sjekkOverlappFor(LocalDate minYtelseDato, LocalDateTimeline<Boolean> ytelseTidslinje, Behandling behandling) {
        List<LocalDateSegment<Boolean>> fpsegments = tilkjentYtelseRepository.hentBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .filter(p -> p.getBeregningsresultatPeriodeTom().isAfter(minYtelseDato.minusDays(1)))
            .map(p -> new LocalDateSegment<>(VirkedagUtil.fomVirkedag(p.getBeregningsresultatPeriodeFom()),
                VirkedagUtil.tomVirkedag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        if (fpsegments.isEmpty())
            return false;

        var fpTidslinje = new LocalDateTimeline<>(fpsegments, StandardCombinators::alwaysTrueForMatch).compress();
        return !fpTidslinje.intersection(ytelseTidslinje).getDatoIntervaller().isEmpty();
    }

    private interface YtelseFeil extends DeklarerteFeil {

        YtelseFeil FACTORY = FeilFactory.create(YtelseFeil.class);

        @TekniskFeil(feilkode = "FP-328773",
            feilmelding = "Vedtatt-Ytelse Feil under parsing av vedtak. key={%s} payload={%s}",
            logLevel = LogLevel.WARN)
        Feil parsingFeil(String key, String payload, IOException e);
    }


    void lagreProsesstaskFor(Behandling behandling, String taskType, int delaysecs) {
        ProsessTaskData data = new ProsessTaskData(taskType);
        data.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        data.setCallIdFraEksisterende();
        data.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(delaysecs));
        prosessTaskRepository.lagre(data);
    }
}
