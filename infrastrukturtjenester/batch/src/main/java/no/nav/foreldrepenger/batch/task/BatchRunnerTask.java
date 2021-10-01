package no.nav.foreldrepenger.batch.task;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.foreldrepenger.batch.feil.BatchFeil;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

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
    static final String BATCH_NAME_RETRY_TASKS = "RETRY_FAILED_TASKS";

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
        var batchParams = prosessTaskData.getPropertyValue(BATCH_PARAMS);
        var batchDate = prosessTaskData.getPropertyValue(BATCH_RUN_DATE);
        if (BATCH_NAME_RETRY_TASKS.equals(batchName)) {
            batchSupportTjeneste.retryAlleProsessTasksFeilet();
            return;
        }
        if (batchDate != null && !batchDate.equals(LocalDate.now().toString())) {
            var logMessage = batchName + " dato passert " + batchDate;
            LOG.warn("Kjører ikke batch {}", logMessage);
            return;
        }
        final var batchTjeneste = batchSupportTjeneste.finnBatchTjenesteForNavn(batchName);
        if (batchTjeneste == null) {
            throw new TekniskException("FP-630260", "Ugyldig job-navn " + batchName);
        }
        final var batchArguments = batchTjeneste.createArguments(parseJobParams(batchParams));

        if (batchArguments.isValid()) {
            var logMessage = batchName + " parametere " + (batchParams != null ? batchParams : "");
            LOG.info("Starter batch {}", logMessage);
            batchTjeneste.launch(batchArguments);
        } else {
            throw BatchFeil.ugyldigeJobParametere(batchArguments);
        }
    }

    private static Map<String, String> parseJobParams(String jobParameters) {
        Map<String, String> resultat = new HashMap<>();
        if (jobParameters != null && jobParameters.length() > 0) {
            var tokenizer = new StringTokenizer(jobParameters, ",");
            while (tokenizer.hasMoreTokens()) {
                var keyValue = tokenizer.nextToken().trim();
                var keyValArr = keyValue.split("=");
                if (keyValArr.length == 2) {
                    resultat.put(keyValArr[0], keyValArr[1]);
                }
            }
        }
        return resultat;
    }
}
