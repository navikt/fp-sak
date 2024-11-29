package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

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
@ProsessTask(value = "fpoversikt.migrering", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FpoversiktMigeringBehandlingHendelseTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FpoversiktMigeringBehandlingHendelseTask.class);
    static final String DATO_KEY = "fom";
    static final String YTELSE_TYPE_KEY = "ytelse";

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
        var ytelseType = prosessTaskData.getPropertyValue(YTELSE_TYPE_KEY);
        LOG.info("Publiser migreringshendelse for saker opprettet {} {}", fom, ytelseType);

        var saker = finnSakerOpprettetPåDato(fom, ytelseType);
        LOG.info("Publiser migreringshendelse for {} saker", saker.size());
        saker.forEach(this::pushKafkaHendelse);
    }

    private List<Long> finnSakerOpprettetPåDato(LocalDate fom, String ytelseType) {
        var sql = """
            select f.id from fagsak f
            where trunc(opprettet_tid) =:fom
            """;
        if (ytelseType != null) {
            sql += " and f.ytelse_type=:ytelseType";
        }
        var query = entityManager.createNativeQuery(sql).setParameter("fom", fom);
        if (ytelseType != null) {
            query.setParameter("ytelseType", ytelseType);
        }
        return query.getResultList()
            .stream()
            .map(num -> Long.parseLong(num.toString()))
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
            .medSaksnummer(behandling.getSaksnummer().getVerdi())
            .build();
        kafkaProducer.sendJsonMedNøkkel(behandling.getSaksnummer().getVerdi(), DefaultJsonMapper.toJson(hendelse));
    }
}
