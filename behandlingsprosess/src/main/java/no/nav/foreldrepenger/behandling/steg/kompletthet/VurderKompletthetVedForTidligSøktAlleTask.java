package no.nav.foreldrepenger.behandling.steg.kompletthet;

import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetVedForTidligSøktSingleTask.BEHANDLING_ID;
import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetVedForTidligSøktSingleTask.DRY_RUN;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;


@ApplicationScoped
@ProsessTask(value = "vurderkompletthet.tidlig.søkt.alle", maxFailedRuns = 1)
public class VurderKompletthetVedForTidligSøktAlleTask implements ProsessTaskHandler {
    private static final Logger LOG = LoggerFactory.getLogger(VurderKompletthetVedForTidligSøktAlleTask.class);

    private EntityManager entityManager;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    VurderKompletthetVedForTidligSøktAlleTask() {
        // for CDI proxy
    }

    @Inject
    public VurderKompletthetVedForTidligSøktAlleTask(EntityManager entityManager, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.entityManager = entityManager;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var dryRun = Optional.ofNullable(prosessTaskData.getPropertyValue(DRY_RUN)).map(Boolean::valueOf).orElse(Boolean.TRUE);

        var query = entityManager.createNativeQuery("""
                select b.id from fpsak.behandling b join fpsak.fagsak f on fagsak_id = f.id
                where b.opprettet_tid >= '19.02.2025'
                and ytelse_type <> 'ES'
                and behandling_type = 'BT-002'
                and behandling_status <> 'AVSLU'
                and b.id not in (select behandling_id from fpsak.aksjonspunkt where aksjonspunkt_def in (7008, 7013) and aksjonspunkt_status = 'OPPR')
            """)
            .setHint(HibernateHints.HINT_READ_ONLY, "true");
        var behandlingIder = query.getResultList();

        LOG.info("KOMPLETTHET_TIDLIG: Fant {} behandlinger som må vurderes på nytt for kompletthet søkt for tidlig", behandlingIder.size());

        for (var behandlingId : behandlingIder) {
            var task = ProsessTaskData.forTaskType(TaskType.forProsessTask(VurderKompletthetVedForTidligSøktSingleTask.class));
            task.setProperty(BEHANDLING_ID, behandlingId.toString());
            task.setProperty(DRY_RUN, dryRun.toString());
            prosessTaskTjeneste.lagre(task);
        }
    }




}
