package no.nav.foreldrepenger.batch;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import no.nav.vedtak.util.FPDateUtil;

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
        for (BatchTjeneste batchTjeneste : batchTjenester) {
            this.batchTjenester.put(batchTjeneste.getBatchName(), batchTjeneste);
        }
        this.prosessTaskRepository = prosessTaskRepository;
    }

    /**
     * Initiell oppretting av BatchSchedulerTask - vil opprette og kjøre en umiddelbart hvis det ikke allerede finnes en KLAR.
     **/
    public void startBatchSchedulerTask() {
        boolean eksisterende = prosessTaskRepository.finnIkkeStartet().stream()
            .map(ProsessTaskData::getTaskType)
            .anyMatch(BatchSchedulerTask.TASKTYPE::equals);
        if (!eksisterende) {
            ProsessTaskData taskData = new ProsessTaskData(BatchSchedulerTask.TASKTYPE);
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
        List<ProsessTaskData> ptdList = this.prosessTaskRepository.finnAlle(ProsessTaskStatus.FEILET);
        if (ptdList.isEmpty()) {
            return;
        }
        LocalDateTime nå = FPDateUtil.nå();
        Map<String, Integer> taskTypesMaxForsøk = new HashMap<>();
        ptdList.stream().map(ProsessTaskData::getTaskType).forEach(tasktype -> {
            if (taskTypesMaxForsøk.get(tasktype) == null) {
                int forsøk = prosessTaskRepository.finnProsessTaskType(tasktype).map(ProsessTaskTypeInfo::getMaksForsøk).orElse(1);
                taskTypesMaxForsøk.put(tasktype, forsøk);
            }
        });
        ptdList.forEach((ptd) -> {
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
