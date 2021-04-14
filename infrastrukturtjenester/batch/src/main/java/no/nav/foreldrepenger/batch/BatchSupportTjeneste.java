package no.nav.foreldrepenger.batch;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.task.BatchSchedulerTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTypeInfo;

@ApplicationScoped
public class BatchSupportTjeneste {

    private ProsessTaskRepository prosessTaskRepository;
    private Map<String, BatchTjeneste> batchTjenester;

    public BatchSupportTjeneste() { //NOSONAR
        // for CDI proxy
        this.batchTjenester = new HashMap<>();
    }

    @Inject
    public BatchSupportTjeneste(ProsessTaskRepository prosessTaskRepository, @Any Instance<BatchTjeneste> batchTjenester) {
        this.batchTjenester = new HashMap<>();
        for (var batchTjeneste : batchTjenester) {
            this.batchTjenester.put(batchTjeneste.getBatchName(), batchTjeneste);
        }
        this.prosessTaskRepository = prosessTaskRepository;
    }

    /**
     * Initiell oppretting av BatchSchedulerTask - vil opprette og kjøre en umiddelbart hvis det ikke allerede finnes en KLAR.
     **/
    public void startBatchSchedulerTask() {
        var eksisterende = prosessTaskRepository.finnIkkeStartet().stream()
            .map(ProsessTaskData::getTaskType)
            .anyMatch(BatchSchedulerTask.TASKTYPE::equals);
        if (!eksisterende) {
            var taskData = new ProsessTaskData(BatchSchedulerTask.TASKTYPE);
            prosessTaskRepository.lagre(taskData);
        }
    }

    /**
     * Opprett en gruppe batchrunners fulgt av en batchscheduler
     **/
    public void opprettScheduledTasks(ProsessTaskGruppe gruppe) {
        prosessTaskRepository.lagre(gruppe);
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
        var ptdList = this.prosessTaskRepository.finnAlle(ProsessTaskStatus.FEILET);
        if (ptdList.isEmpty()) {
            return;
        }

        var filtrerteTask = ptdList.stream()
            .filter(ptd -> !ptd.getTaskType().equals("iverksetteVedtak.oppdragTilØkonomi"))
            .collect(Collectors.toList());

        var nå = LocalDateTime.now();
        Map<String, Integer> taskTypesMaxForsøk = new HashMap<>();
        filtrerteTask.stream().map(ProsessTaskData::getTaskType).forEach(tasktype -> {
            if (taskTypesMaxForsøk.get(tasktype) == null) {
                int forsøk = prosessTaskRepository.finnProsessTaskType(tasktype).map(ProsessTaskTypeInfo::getMaksForsøk).orElse(1);
                taskTypesMaxForsøk.put(tasktype, forsøk);
            }
        });
        filtrerteTask.forEach((ptd) -> {
            ptd.setStatus(ProsessTaskStatus.KLAR);
            ptd.setNesteKjøringEtter(nå);
            ptd.setSisteFeilKode(null);
            ptd.setSisteFeil(null);
            if (taskTypesMaxForsøk.get(ptd.getTaskType()).equals(ptd.getAntallFeiledeForsøk())) {
                ptd.setAntallFeiledeForsøk(ptd.getAntallFeiledeForsøk() - 1);
            }
            this.prosessTaskRepository.lagre(ptd);
        });
    }

}
