package no.nav.foreldrepenger.mottak.vedtak.kafka;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.MottattVedtak;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.HåndterOverlappPleiepengerTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.log.util.LoggerUtils;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);

    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final Set<Ytelser> EKSTERNE_HÅNDTERES = Set.of(Ytelser.PLEIEPENGER_SYKT_BARN, Ytelser.PLEIEPENGER_NÆRSTÅENDE);
    private static final Set<Ytelser> EKSTERNE_LOGGES = Set.of(Ytelser.FRISINN, Ytelser.OMSORGSPENGER);

    private FagsakTjeneste fagsakTjeneste;
    private LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private HendelsemottakRepository mottakRepository;

    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(FagsakTjeneste fagsakTjeneste, BeregningsresultatRepository tilkjentYtelseRepository,
                                    BehandlingRepository behandlingRepository,
                                    LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger,
                                    ProsessTaskTjeneste taskTjeneste,
                                    HendelsemottakRepository mottakRepository) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.eksternOverlappLogger = eksternOverlappLogger;
        this.behandlingRepository = behandlingRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.taskTjeneste = taskTjeneste;
        this.mottakRepository = mottakRepository;
    }

    void handleMessage(String key, String payload) {
        // enhver exception ut fra denne metoden medfører at tråden som leser fra kafka gir opp og dør på seg.
        try {
            var mottattVedtak = StandardJsonConfig.fromJson(payload, Ytelse.class);
            if (mottattVedtak != null) {
                handleMessageIntern((YtelseV1) mottattVedtak);
            }
        } catch (VLException e) {
            LOG.warn("FP-328773 Vedtatt-Ytelse Feil under parsing av vedtak. key={} payload={}", key, payload, e);
        } catch (Exception e) {
            LOG.warn("Vedtatt-Ytelse exception ved håndtering av vedtaksmelding, ignorerer key={}", LoggerUtils.removeLineBreaks(payload), e);
        }
    }

    void handleMessageIntern(YtelseV1 ytelse) {
        if (Kildesystem.FPSAK.equals(ytelse.getKildesystem())) {
            return;
        }

        /* Re-Enable ved flytting av Kafka til Aiven eller mottak fra nye system/ytelser
            var hendelseId = ytelse.getKildesystem().name() + ytelse.getVedtakReferanse() + Optional.ofNullable(ytelse.getSaksnummer()).orElse("") + ytelse.getYtelse().name();
            if (hendelseId.length() > 95) hendelseId = hendelseId.substring(0, 92);
            if (!mottakRepository.hendelseErNy(hendelseId)) {
                LOG.warn("KAFKA Mottatt vedtakshendelse på nytt hendelse={}", hendelseId);
                return;
            }
            mottakRepository.registrerMottattHendelse(hendelseId);
         */

        var fagsaker = getFagsakerFor(ytelse);
        if (!fagsaker.isEmpty()) {
            lagreMottattVedtak(ytelse);
        }
        if (skalLoggeOverlappDB(ytelse)) {
            loggVedtakOverlapp(ytelse, fagsaker);
        } else if (EKSTERNE_HÅNDTERES.contains(ytelse.getYtelse())) {
            var callID = UUID.randomUUID();
            fagsakerMedVedtakOverlapp(ytelse, fagsaker).forEach(f -> opprettHåndterOverlappTaskPleiepenger(f, callID));
        } else {
            LOG.info("Vedtatt-Ytelse mottok vedtak fra system {} saksnummer {} ytelse {}", ytelse.getKildesystem(),
                ytelse.getSaksnummer(), ytelse.getYtelse());
            LOG.info("Vedtatt-Ytelse VL har disse sakene for bruker med vedtak {} - saker {}",
                ytelse.getYtelse(), fagsaker.stream().map(Fagsak::getSaksnummer).collect(Collectors.toList()));

            var fagsakerMedOverlapp = fagsakerMedVedtakOverlapp(ytelse, fagsaker);
            if (!fagsakerMedOverlapp.isEmpty()) {
                var overlappSaksnummerList =
                    fagsakerMedOverlapp.stream().map(Fagsak::getSaksnummer).map(Saksnummer::getVerdi).collect(Collectors.toList());
                var beskrivelse = String.format("Vedtak om %s sak %s overlapper saker i VL: %s",
                    ytelse.getYtelse(), ytelse.getSaksnummer(), String.join(", ", overlappSaksnummerList));
                LOG.warn("Vedtatt-Ytelse KONTAKT PRODUKTEIER UMIDDELBART! - {}", beskrivelse);
                loggVedtakOverlapp(ytelse, fagsakerMedOverlapp);
            }
        }
    }

    private boolean skalLoggeOverlappDB(YtelseV1 ytelse) {
        return EKSTERNE_LOGGES.contains(ytelse.getYtelse());
    }

    private void lagreMottattVedtak(YtelseV1 ytelse) {
        var mottattVedtakBuilder = MottattVedtak.builder()
            .medFagsystem(ytelse.getKildesystem().name())
            .medReferanse(ytelse.getVedtakReferanse())
            .medYtelse(ytelse.getYtelse().name())
            .medSaksnummer(ytelse.getSaksnummer());
        mottakRepository.registrerMottattVedtak(mottattVedtakBuilder.build());
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

}
