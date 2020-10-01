package no.nav.foreldrepenger.domene.vedtak.migrering;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.folketrygdloven.kalkulator.JsonMapper;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.vedtak.observer.HendelseProducer;
import no.nav.foreldrepenger.domene.vedtak.observer.PubliserVedtattYtelseHendelseFeil;
import no.nav.foreldrepenger.domene.vedtak.observer.VedtattYtelseTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@ProsessTask(AnnonserVedtakTilAbakusTask.ABAKUS_VEDTAK_MIGRERING)
public class AnnonserVedtakTilAbakusTask implements ProsessTaskHandler {

    public static final String MIGRERING_START_FAGSAKID = "migrering.start.fagsakid";
    public static final String ABAKUS_VEDTAK_MIGRERING = "abakus.vedtak.migrering";
    public static final int MAX_RESULT = 500;
    private static final Logger log = LoggerFactory.getLogger(AnnonserVedtakTilAbakusTask.class);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskRepository taskRepository;
    private VedtattYtelseTjeneste vedtattYtelseTjeneste;
    private HendelseProducer producer;

    public AnnonserVedtakTilAbakusTask() {
    }

    @Inject
    public AnnonserVedtakTilAbakusTask(FagsakRepository fagsakRepository,  // NOSONAR
                                       BehandlingRepository behandlingRepository,
                                       ProsessTaskRepository taskRepository,
                                       VedtattYtelseTjeneste vedtattYtelseTjeneste,
                                       @KonfigVerdi("kafka.fattevedtak.topic") String topicName,
                                       @KonfigVerdi("bootstrap.servers") String bootstrapServers,
                                       @KonfigVerdi("schema.registry.url") String schemaRegistryUrl,
                                       @KonfigVerdi("systembruker.username") String username,
                                       @KonfigVerdi("systembruker.password") String password) { // NOSONAR
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.taskRepository = taskRepository;
        this.vedtattYtelseTjeneste = vedtattYtelseTjeneste;
        this.producer = new HendelseProducer(topicName, bootstrapServers, schemaRegistryUrl, username, password);
    }

    @Override
    public void doTask(ProsessTaskData data) {
        final String fagsakIdStart = data.getPropertyValue(MIGRERING_START_FAGSAKID) == null ? "0" : data.getPropertyValue(MIGRERING_START_FAGSAKID);
        long orginalFagsakId = Long.parseLong(fagsakIdStart);
        long sisteFagsakId = Long.parseLong(fagsakIdStart);
        log.info("[ABAKUS-MIGRERING] Starter med fagsaker fra id : {}", sisteFagsakId);
        final List<Fagsak> fagsaker = fagsakRepository.hentFagsakerMedIdStørreEnn(sisteFagsakId, MAX_RESULT);

        for (Fagsak fagsak : fagsaker) {
            final Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
            if (behandling.isPresent()) {
                Behandling behandlingen = behandling.get();
                final Ytelse ytelse = vedtattYtelseTjeneste.genererYtelse(behandlingen);
                producer.sendJsonMedNøkkel(behandlingen.getUuid().toString(), JsonMapper.toJson(ytelse, PubliserVedtattYtelseHendelseFeil.FEILFACTORY::kanIkkeSerialisere));
            }
            sisteFagsakId = fagsak.getId();
        }

        if (orginalFagsakId != sisteFagsakId) {
            log.info("[ABAKUS-MIGRERING] Ferdig med fagsaker fra id : {} Schedulerer ny task.", orginalFagsakId);
            final ProsessTaskData taskData = new ProsessTaskData(ABAKUS_VEDTAK_MIGRERING);
            taskData.setProperty(MIGRERING_START_FAGSAKID, "" + sisteFagsakId);
            taskRepository.lagre(taskData);
        } else {
            log.info("[ABAKUS-MIGRERING] Samtlige vedtak overført. sisteFagsakId={}", sisteFagsakId);
        }
    }
}
