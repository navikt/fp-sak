package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.hendelser.behandling.Hendelse;
import no.nav.vedtak.hendelser.behandling.v1.BehandlingHendelseV1;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

@Dependent
@ProsessTask(value = "fpoversikt.migrering", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FpoversiktMigeringBehandlingHendelseTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FpoversiktMigeringBehandlingHendelseTask.class);
    static final String DATO_KEY = "fom";

    private final BehandlingRepository behandlingRepository;
    private final EntityManager entityManager;
    private final FpoversiktHendelseProducer kafkaProducer;

    @Inject
    public FpoversiktMigeringBehandlingHendelseTask(BehandlingRepository behandlingRepository,
                                                    EntityManager entityManager,
                                                    FpoversiktHendelseProducer kafkaProducer) {
        this.behandlingRepository = behandlingRepository;
        this.entityManager = entityManager;
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var fom = LocalDate.parse(prosessTaskData.getPropertyValue(DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        LOG.info("Publiser migreringshendelse for saker opprettet {}", fom);

        var saker = finnSakerOpprettetPåDato(fom);
        LOG.info("Publiser migreringshendelse for {} saker", saker.size());
        saker.forEach(this::pushKafkaHendelse);
    }

    private List<Long> finnSakerOpprettetPåDato(LocalDate fom) {
        return entityManager.createNativeQuery("""
            select f.id from fagsak f
            where trunc(opprettet_tid) =:fom
            """)
            .setParameter("fom", fom)
            .getResultList()
            .stream()
            .map(bd -> ((BigDecimal)bd).longValue())
            .toList();
    }

    private void pushKafkaHendelse(Long fagsakId) {
        var behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsakId);
        if (behandlingOpt.isEmpty()) {
            LOG.info("Publiser migreringshendelse, ingen behandling for fagsak {}", fagsakId);
            return;
        }
        var behandling = behandlingOpt.get();
        LOG.info("Publiser migreringshendelse på kafka for fagsak {} {}", fagsakId, behandling.getId());
        var hendelse = new BehandlingHendelseV1.Builder()
            .medHendelse(Hendelse.MIGRERING)
            .medHendelseUuid(UUID.randomUUID())
            .medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi())
            .build();
        kafkaProducer.sendJsonMedNøkkel(behandling.getFagsak().getSaksnummer().getVerdi(), DefaultJsonMapper.toJson(hendelse));
    }
}
