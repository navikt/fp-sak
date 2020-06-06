package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskEtterkontrollTjeneste {

    private ProsessTaskRepository prosessTaskRepository;
    private EtterkontrollRepository etterkontrollRepository;

    AutomatiskEtterkontrollTjeneste() {
        // For CDI?
    }

    @Inject
    public AutomatiskEtterkontrollTjeneste(ProsessTaskRepository prosessTaskRepository,
            EtterkontrollRepository etterkontrollRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.etterkontrollRepository = etterkontrollRepository;
    }

    public void etterkontrollerBehandlinger() {
        // Etterkontrolltidspunkt er allerede satt 60D fram i EK-repo
        List<Behandling> kontrollKandidater = etterkontrollRepository.finnKandidaterForAutomatiskEtterkontroll();

        String callId = (MDCOperations.getCallId() != null ? MDCOperations.getCallId() : MDCOperations.generateCallId()) + "_";

        for (Behandling kandidat : kontrollKandidater) {
            String nyCallId = callId + kandidat.getId();
            opprettEtterkontrollTask(kandidat, nyCallId);
        }
    }

    private void opprettEtterkontrollTask(Behandling kandidat, String callId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(kandidat.getFagsakId(), kandidat.getId(), kandidat.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setCallId(callId);
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public List<TaskStatus> hentStatusForEtterkontrollGruppe(String gruppe) {
        return prosessTaskRepository.finnStatusForTaskIGruppe(AutomatiskEtterkontrollTask.TASKTYPE, gruppe);
    }

}
