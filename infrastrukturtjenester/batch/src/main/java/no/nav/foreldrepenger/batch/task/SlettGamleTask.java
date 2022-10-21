package no.nav.foreldrepenger.batch.task;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Opp
 */
@Dependent
@ProsessTask(value = "batch.tasks.deleteold", maxFailedRuns = 1)
public class SlettGamleTask implements ProsessTaskHandler {

    private final BatchSupportTjeneste batchTeneste;

    @Inject
    public SlettGamleTask(BatchSupportTjeneste batchTeneste) {
        this.batchTeneste = batchTeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        batchTeneste.slettGamleTasks();
    }

}
