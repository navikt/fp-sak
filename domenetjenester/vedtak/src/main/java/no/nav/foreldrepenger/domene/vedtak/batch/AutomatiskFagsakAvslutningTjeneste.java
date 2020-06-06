package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.vedtak.intern.AutomatiskFagsakAvslutningTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskFagsakAvslutningTjeneste {

    private ProsessTaskRepository prosessTaskRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    AutomatiskFagsakAvslutningTjeneste() {
        // For CDI?
    }

    @Inject
    public AutomatiskFagsakAvslutningTjeneste(ProsessTaskRepository prosessTaskRepository,
                                              FagsakRelasjonRepository fagsakRelasjonRepository) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
    }

    String avsluttFagsaker(String batchname, LocalDate date) {
        List<Fagsak> fagsaker = fagsakRelasjonRepository.finnFagsakerForAvsluttning(date);

        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (Fagsak fagsak : fagsaker) {
            List<ProsessTaskData> tasks = new ArrayList<>();

            String nyCallId = callId + fagsak.getId();
            tasks.add(opprettFagsakAvslutningTask(fagsak, nyCallId));

            if (!tasks.isEmpty()) {
                tasks.forEach(t -> t.setPrioritet(100));
                ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
                tasks.forEach(gruppe::addNesteSekvensiell);
                prosessTaskRepository.lagre(gruppe);
            }
        }
        return batchname + "-" + (UUID.randomUUID().toString());
    }

    private ProsessTaskData opprettFagsakAvslutningTask(Fagsak fagsak, String callId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskFagsakAvslutningTask.TASKTYPE);
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setPrioritet(100);
        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);
        return prosessTaskData;
    }

    public List<TaskStatus> hentStatusForFagsakAvslutningGruppe(String gruppe) {
        return prosessTaskRepository.finnStatusForTaskIGruppe(AutomatiskFagsakAvslutningTask.TASKTYPE, gruppe);
    }
}
