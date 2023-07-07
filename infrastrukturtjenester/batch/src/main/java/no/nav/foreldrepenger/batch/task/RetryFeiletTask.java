package no.nav.foreldrepenger.batch.task;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Opp
 */
@Dependent
@ProsessTask(value = "batch.tasks.retryfailed", maxFailedRuns = 1)
public class RetryFeiletTask implements ProsessTaskHandler {

    private final BatchSupportTjeneste batchTjeneste;

    @Inject
    public RetryFeiletTask(BatchSupportTjeneste batchTjeneste) {
        this.batchTjeneste = batchTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        batchTjeneste.retryAlleProsessTasksFeilet();
    }

}
