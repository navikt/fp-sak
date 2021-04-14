package no.nav.foreldrepenger.domene.vedtak.task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingIdFagsakIdAktorId;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(MigrerTilkjentYtelseTask.TASKTYPE)
public class MigrerTilkjentYtelseTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MigrerTilkjentYtelseTask.class);
    public static final String TASKTYPE = "migrer.sendTilkjentYtelse";

    private static int ANTALL_PR_RUNDE = 10;
    private static Duration DELAY_MELLOM_KJØRINGER = Duration.ofSeconds(10);

    private TilkjentYtelseMeldingProducer meldingProducer;
    private MigrerBehandlingRepository behandlingRepository;
    private ProsessTaskRepository prosessTaskRepository;


    public MigrerTilkjentYtelseTask() {
        // CDI krav
    }

    @Inject
    public MigrerTilkjentYtelseTask(TilkjentYtelseMeldingProducer meldingProducer, MigrerBehandlingRepository behandlingRepository, ProsessTaskRepository prosessTaskRepository) {
        this.meldingProducer = meldingProducer;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var verdi = prosessTaskData.getPropertyValue("behandlingIdTak");
        Long sisteBehandlingId = verdi == null ? Long.MAX_VALUE : Long.valueOf(verdi);

        var behandlinger = behandlingRepository.hentBehandlingerForMigrering(sisteBehandlingId, ANTALL_PR_RUNDE);
        LOG.info("overfører {} behandlinger", behandlinger.size());
        for (var behandling : behandlinger) {
            LOG.info("Overfører behandlingId={} behandlingUuid={}", behandling.getBehandlingId(), behandling.getBehandlingUuid());

            var fagsakYtelseType = behandling.getFagsakYtelseType();
            var aktørId = new AktørId(behandling.getAktorId());
            meldingProducer.sendTilkjentYtelse(fagsakYtelseType, behandling.getSaksnummer(), aktørId, behandling.getBehandlingId(), behandling.getBehandlingUuid());
        }

        if (behandlinger.size() == ANTALL_PR_RUNDE) {
            var data = new ProsessTaskData(TASKTYPE);
            data.setProperty("behandlingIdTak", Long.toString(behandlinger.get(ANTALL_PR_RUNDE - 1).getBehandlingId()));
            data.setNesteKjøringEtter(LocalDateTime.now().plus(DELAY_MELLOM_KJØRINGER));
            prosessTaskRepository.lagre(data);
        } else {
            LOG.info("siste migrer.sendTilkjentYtelse er ferdig");
        }

    }

    @ApplicationScoped
    public static class MigrerBehandlingRepository {

        private EntityManager entityManager;

        @Inject
        public MigrerBehandlingRepository( EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        MigrerBehandlingRepository() {

        }

        @SuppressWarnings({"cast", "unchecked"})
        public List<BehandlingIdFagsakIdAktorId> hentBehandlingerForMigrering(Long sisteBehandlingId, long antall) {
            Objects.requireNonNull(sisteBehandlingId, "behandlingId"); //NOSONAR

            var sql = "select ytelseType, saksnummer, aktorId, behandlingId, behandlingUuid from " +
                " (select f.ytelse_type ytelseType, f.saksnummer saksnummer, br.aktoer_id aktorId, b.id behandlingId, b.uuid behandlingUuid" +
                "    from behandling b " +
                "    join fagsak f on b.fagsak_id = f.id " +
                "    join bruker br on br.id = f.bruker_id " +
                "    where " +
                "         b.behandling_type in ('BT-002', 'BT-004')" + //kun ytelses-vedtak
                "     and b.behandling_status in ('IVED', 'AVSLU') " + //kun ferdig fattede vedtak
                "     and b.id < :sisteBehandlingId " +
                "    order by b.id desc) " +
                " where rownum <= :antall ";

            var query = entityManager.createNativeQuery(sql, "BehandlingIdFagsakIdAktoerId");
            query.setParameter("sisteBehandlingId", sisteBehandlingId);
            query.setParameter("antall", antall);

            return (List<BehandlingIdFagsakIdAktorId>) query.getResultList();
        }


    }

}
