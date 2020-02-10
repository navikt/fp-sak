package no.nav.foreldrepenger.mottak.publiserer.observer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokumentPersistertEvent;
import no.nav.foreldrepenger.mottak.publiserer.task.PubliserPersistertDokumentHendelseTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class MottattDokumentPersistertEventObserver {

    private ProsessTaskRepository taskRepository;

    public MottattDokumentPersistertEventObserver() {
    }

    @Inject
    public MottattDokumentPersistertEventObserver(ProsessTaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void observerMottattDokumentPersistert(@Observes MottattDokumentPersistertEvent event) {
        if (DokumentTypeId.INNTEKTSMELDING.equals(event.getMottattDokument().getDokumentType())) {
            final ProsessTaskData taskData = new ProsessTaskData(PubliserPersistertDokumentHendelseTask.TASKTYPE);
            taskData.setBehandling(event.getFagsakId(), event.getBehandlingId(), event.getAktørId().getId());
            taskData.setProperty(PubliserPersistertDokumentHendelseTask.MOTTATT_DOKUMENT_ID_KEY, event.getMottattDokument().getId().toString());
            taskData.setCallIdFraEksisterende();
            taskRepository.lagre(taskData);
        }
    }
}
