package no.nav.foreldrepenger.mottak.vedtak.kafka;

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
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelser;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);
    private static final ObjectMapper OBJECT_MAPPER = JacksonJsonConfig.getMapper();

    private FagsakTjeneste fagsakTjeneste;
    private VurderOpphørAvYtelser vurderOpphørAvYtelser;
    private LoggOverlappendeEksternYtelseTjeneste eksternOverlappLogger;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private Validator validator;
    private ProsessTaskRepository prosessTaskRepository;

    private static final Map<YtelseType, FagsakYtelseType > YTELSE_TYPE_MAP = Map.of(
        YtelseType.ENGANGSTØNAD, FagsakYtelseType.ENGANGSTØNAD,
        YtelseType.FORELDREPENGER, FagsakYtelseType.FORELDREPENGER,
        YtelseType.SVANGERSKAPSPENGER, FagsakYtelseType.SVANGERSKAPSPENGER
        );

    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(FagsakTjeneste fagsakTjeneste,
                                    VurderOpphørAvYtelser vurderOpphørAvYtelser,
                                    LoggOverlappendeEksternYtelseTjeneste eksternOverlappLogger,
                                    BehandlingRepositoryProvider repositoryProvider,
                                    ProsessTaskRepository prosessTaskRepository) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.vurderOpphørAvYtelser = vurderOpphørAvYtelser;
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

        if (Fagsystem.FPSAK.equals(ytelse.getFagsystem())){
            oprettTasksForFpsakVedtak(ytelse);
        }
        else {
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

        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            opprettTaskForBerørtBehandling(behandling);
            opprettTaskForOpphørAvYtelser(behandling);
        } //SVP
        else {
            opprettTaskForOpphørAvYtelser(behandling);
        }
    }

    void sjekkVedtakOverlapp(YtelseV1 ytelse) {
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
        if (YtelseType.FRISINN.equals(ytelse.getType())) {
            eksternOverlappLogger.loggOverlappUtenGradering(ytelse, minYtelseDato, ytelseTidslinje, fagsaker);
            return;
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
            logLevel = LogLevel.INFO)
        Feil parsingFeil(String key, String payload, IOException e);
    }

    void opprettTaskForBerørtBehandling(Behandling behandling) {
        ProsessTaskData berørtBehandlingTask = new ProsessTaskData(StartBerørtBehandlingTask.TASKTYPE);
        berørtBehandlingTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        berørtBehandlingTask.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(berørtBehandlingTask);
    }

    void opprettTaskForOpphørAvYtelser(Behandling behandling) {
        ProsessTaskData vurderOpphør = new ProsessTaskData(VurderOpphørAvYtelserTask.TASKTYPE);
        vurderOpphør.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        vurderOpphør.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(vurderOpphør);
    }
}
