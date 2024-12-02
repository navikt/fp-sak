package no.nav.foreldrepenger.mottak.vedtak.kafka;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.MottattVedtak;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.HåndterOverlappPleiepengerTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.LoggOverlappEksterneYtelserTjeneste;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.SjekkOverlapp;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaMessageHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.util.LoggerUtils;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class VedtaksHendelseHåndterer implements KafkaMessageHandler.KafkaStringMessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksHendelseHåndterer.class);
    private static final String GROUP_ID = "fpsak-vedtakfattet";  // Hold konstant pga offset commit !!

    private static final Set<FagsakYtelseType> VURDER_OVERLAPP = Set.of(FagsakYtelseType.FORELDREPENGER, FagsakYtelseType.SVANGERSKAPSPENGER);
    private static final Set<Ytelser> EKSTERNE_HÅNDTERES = Set.of(Ytelser.PLEIEPENGER_SYKT_BARN, Ytelser.PLEIEPENGER_NÆRSTÅENDE);
    private static final Set<Ytelser> EKSTERNE_LOGGES = Set.of(Ytelser.FRISINN, Ytelser.OMSORGSPENGER);

    private String topicName;
    private FagsakTjeneste fagsakTjeneste;
    private LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private HendelsemottakRepository mottakRepository;

    public VedtaksHendelseHåndterer() {
    }

    @Inject
    public VedtaksHendelseHåndterer(@KonfigVerdi("kafka.fattevedtak.topic") String topicName,
                                    FagsakTjeneste fagsakTjeneste,
                                    BeregningsresultatRepository tilkjentYtelseRepository,
                                    BehandlingRepository behandlingRepository,
                                    LoggOverlappEksterneYtelserTjeneste eksternOverlappLogger,
                                    ProsessTaskTjeneste taskTjeneste,
                                    HendelsemottakRepository mottakRepository) {
        this.topicName = topicName;
        this.fagsakTjeneste = fagsakTjeneste;
        this.eksternOverlappLogger = eksternOverlappLogger;
        this.behandlingRepository = behandlingRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.taskTjeneste = taskTjeneste;
        this.mottakRepository = mottakRepository;
    }

    @Override
    public void handleRecord(String key, String value) {
        // enhver exception ut fra denne metoden medfører at tråden som leser fra kafka gir opp og dør på seg.
        try {
            var mottattVedtak = StandardJsonConfig.fromJson(value, Ytelse.class);
            if (mottattVedtak != null) {
                handleMessageIntern((YtelseV1) mottattVedtak);
            }
        } catch (VLException e) {
            LOG.warn("FP-328773 Vedtatt-Ytelse Feil under parsing av vedtak. key={} payload={}", key, value, e);
        } catch (Exception e) {
            LOG.warn("Vedtatt-Ytelse exception ved håndtering av vedtaksmelding, ignorerer key={}", LoggerUtils.removeLineBreaks(value), e);
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
            LOG.info("Vedtatt-Ytelse mottok vedtak fra system {} saksnummer {} ytelse {}", ytelse.getKildesystem(), ytelse.getSaksnummer(),
                ytelse.getYtelse());
            LOG.info("Vedtatt-Ytelse VL har disse sakene for bruker med vedtak {} - saker {}", ytelse.getYtelse(),
                fagsaker.stream().map(Fagsak::getSaksnummer).toList());

            var fagsakerMedOverlapp = fagsakerMedVedtakOverlapp(ytelse, fagsaker);
            if (!fagsakerMedOverlapp.isEmpty()) {
                var overlappSaksnummerList = fagsakerMedOverlapp.stream().map(Fagsak::getSaksnummer).map(Saksnummer::getVerdi).toList();
                var beskrivelse = String.format("Vedtak om %s sak %s overlapper saker i VL: %s", ytelse.getYtelse(), ytelse.getSaksnummer(),
                    String.join(", ", overlappSaksnummerList));
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
        // Kjøretidspunkt tidlig neste virkedag slik at OS har fordøyd oppdrag fra K9Sak men ikke utbetalt ennå
        var nesteFormiddag = LocalDateTime.of(VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1)), LocalTime.of(7, 35, 1));
        var prosessTaskData = ProsessTaskData.forProsessTask(HåndterOverlappPleiepengerTask.class);
        prosessTaskData.setFagsak(f.getSaksnummer().getVerdi(), f.getId());
        // Gi abakus tid til å konsumere samme hendelse så det finnes et grunnlag å hente opp.
        prosessTaskData.setNesteKjøringEtter(nesteFormiddag);
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
        if (fagsaker.isEmpty()) {
            return List.of();
        }
        var behandlinger = fagsaker.stream()
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .filter(b -> SjekkOverlapp.erOverlappOgMerEnn100Prosent(tilkjentYtelseRepository.hentUtbetBeregningsresultat(b.getId()), List.of(ytelse)));

        return behandlinger.map(Behandling::getFagsak).toList();
    }

    private List<Fagsak> getFagsakerFor(YtelseV1 ytelse) {
        return fagsakTjeneste.finnFagsakerForAktør(new AktørId(ytelse.getAktør().getVerdi()))
            .stream()
            .filter(f -> VURDER_OVERLAPP.contains(f.getYtelseType()))
            .toList();
    }


    @Override
    public String topic() {
        return topicName;
    }

    @Override
    public String groupId() {
        return GROUP_ID;
    }
}
