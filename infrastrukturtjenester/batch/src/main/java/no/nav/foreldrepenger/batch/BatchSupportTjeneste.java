package no.nav.foreldrepenger.batch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.batch.task.BatchSchedulerTask;
import no.nav.vedtak.felles.prosesstask.api.*;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class BatchSupportTjeneste {

    private ProsessTaskTjeneste taskTjeneste;
    private Map<String, BatchTjeneste> batchTjenester;

    public BatchSupportTjeneste() {
        // for CDI proxy
        this.batchTjenester = new HashMap<>();
    }

    @Inject
    public BatchSupportTjeneste(ProsessTaskTjeneste taskTjeneste, @Any Instance<BatchTjeneste> batchTjenester) {
        this.batchTjenester = new HashMap<>();
        for (var batchTjeneste : batchTjenester) {
            this.batchTjenester.put(batchTjeneste.getBatchName(), batchTjeneste);
        }
        this.taskTjeneste = taskTjeneste;
    }

    /**
     * Initiell oppretting av BatchSchedulerTask - vil opprette og kjøre en umiddelbart hvis det ikke allerede finnes en KLAR.
     **/
    public void startBatchSchedulerTask() {
        var schedulerType = TaskType.forProsessTask(BatchSchedulerTask.class);
        var eksisterende = taskTjeneste.finnAlle(ProsessTaskStatus.KLAR).stream()
            .map(ProsessTaskData::taskType)
            .anyMatch(schedulerType::equals);
        if (!eksisterende) {
            var taskData = ProsessTaskData.forProsessTask(BatchSchedulerTask.class);
            taskTjeneste.lagre(taskData);
        }
    }

    /**
     * Opprett en gruppe batchrunners fulgt av en batchscheduler
     **/
    public void opprettScheduledTasks(ProsessTaskGruppe gruppe) {
        taskTjeneste.lagre(gruppe);
    }

    /**
     * Finn riktig batchtjeneste for oppgitt batchnavn.
     */
    public BatchTjeneste finnBatchTjenesteForNavn(String batchNavn) {
        return batchTjenester.get(batchNavn);
    }

    /**
     * Prøv å kjøre feilete tasks på nytt - restart av andre system.
     */
    public void retryAlleProsessTasksFeilet() {
        taskTjeneste.restartAlleFeiledeTasks();
    }

    /**
     * Prøv å kjøre feilete tasks på nytt - restart av andre system.
     */
    public void slettGamleTasks() {
        taskTjeneste.slettÅrsgamleFerdige();
    }

}
