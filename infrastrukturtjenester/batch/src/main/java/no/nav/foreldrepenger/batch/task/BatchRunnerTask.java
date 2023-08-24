package no.nav.foreldrepenger.batch.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Opp
 */
@ApplicationScoped
@ProsessTask(value = "batch.runner", maxFailedRuns = 1)
public class BatchRunnerTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BatchRunnerTask.class);

    static final String BATCH_NAME = "batch.runner.name";
    static final String BATCH_PARAMS = "batch.runner.params";
    static final String BATCH_RUN_DATE = "batch.runner.onlydate";

    private BatchSupportTjeneste batchSupportTjeneste;

    BatchRunnerTask() {
        // for CDI proxy
    }

    @Inject
    public BatchRunnerTask(BatchSupportTjeneste batchSupportTjeneste) {
        this.batchSupportTjeneste = batchSupportTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var batchName = prosessTaskData.getPropertyValue(BATCH_NAME);
        var batchDate = prosessTaskData.getPropertyValue(BATCH_RUN_DATE);
        if (batchDate != null && !batchDate.equals(LocalDate.now().toString())) {
            var logMessage = batchName + " dato passert " + batchDate;
            LOG.warn("Kjører ikke batch {}", logMessage);
            return;
        }
        var batchTjeneste = batchSupportTjeneste.finnBatchTjenesteForNavn(batchName);
        if (batchTjeneste == null) {
            throw new TekniskException("FP-630260", "Ugyldig job-navn " + batchName);
        }

        LOG.info("Starter batch {}", batchName);
        batchTjeneste.launch(prosessTaskData.getProperties());
    }

}
