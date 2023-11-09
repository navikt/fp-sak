package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.query.NativeQuery;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
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
                    + " WHERE pt.status in ('KLAR', 'FEILET')"
                    + " AND pt.task_type in ('vedtak.overlapp.avstem', 'vedtak.overlapp.periode')"
                    + " FOR UPDATE SKIP LOCKED ",
                ProsessTaskEntitet.class)
            .setMaxResults(2000);


        var resultList = query.getResultList().stream().map(ProsessTaskEntitet::tilProsessTask).toList();
        resultList.forEach(task -> entityManager.createNativeQuery("update prosess_task set status = 'FERDIG' where id = :tid").setParameter("tid", task.getId()).executeUpdate());
    }

}
