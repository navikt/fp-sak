package no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.tjeneste;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.task.SendForlengelsesbrevTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
public class SendForlengelsesbrevTjeneste {

    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;

    SendForlengelsesbrevTjeneste() {
        //For CDI?
    }

    @Inject
    public SendForlengelsesbrevTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository, ProsessTaskTjeneste taskTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.taskTjeneste = taskTjeneste;
    }

    public String sendForlengelsesbrev() {
        var kandidater = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();
        var taskGruppe = UUID.randomUUID().toString();
        kandidater.forEach(kandidat -> opprettSendForlengelsesbrevTask(kandidat, taskGruppe));
        return taskGruppe;
    }

    private void opprettSendForlengelsesbrevTask(Behandling behandling, String taskGruppe) {
        var prosessTaskData = ProsessTaskData.forProsessTask(SendForlengelsesbrevTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setGruppe(taskGruppe);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }
}
