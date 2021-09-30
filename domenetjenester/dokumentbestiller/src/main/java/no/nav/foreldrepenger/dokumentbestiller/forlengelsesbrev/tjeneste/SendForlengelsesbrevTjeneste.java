package no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.tjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.dokumentbestiller.forlengelsesbrev.task.SendForlengelsesbrevTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class SendForlengelsesbrevTjeneste {

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;

    SendForlengelsesbrevTjeneste() {
        //For CDI?
    }

    @Inject
    public SendForlengelsesbrevTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public String sendForlengelsesbrev() {
        var kandidater = behandlingKandidaterRepository.finnBehandlingerMedUtløptBehandlingsfrist();
        String gruppe = null;
        for (var kandidat : kandidater) {
            gruppe = opprettSendForlengelsesbrevTask(kandidat);
        }
        //TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return gruppe == null ? "0" : gruppe;
    }

    private String opprettSendForlengelsesbrevTask(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(SendForlengelsesbrevTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
        return prosessTaskData.getGruppe();
    }
}
