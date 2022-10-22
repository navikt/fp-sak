package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.query.NativeQuery;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskEntitet;

@ApplicationScoped
@ProsessTask(value = "vedtak.avstem.avbryt", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AvbrytAvstemTask implements ProsessTaskHandler {

    private EntityManager entityManager;
    private ProsessTaskTjeneste taskTjeneste;

    AvbrytAvstemTask() {
        // for CDI proxy
    }

    @Inject
    public AvbrytAvstemTask(EntityManager entityManager, ProsessTaskTjeneste taskTjeneste) {
        super();
        this.entityManager = entityManager;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        @SuppressWarnings("unchecked") var query = (NativeQuery<ProsessTaskEntitet>) entityManager
            .createNativeQuery(
                "SELECT pt.* FROM PROSESS_TASK pt"
                    + " WHERE pt.status = 'KLAR'"
                    + " AND pt.task_type in ('vedtak.overlapp.avstem', 'vedtak.overlapp.periode')"
                    + " FOR UPDATE SKIP LOCKED ",
                ProsessTaskEntitet.class);


        var resultList = query.getResultList();
        resultList.forEach(task -> taskTjeneste.setProsessTaskFerdig(task.getId(), ProsessTaskStatus.KLAR));
    }
    
}
