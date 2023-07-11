package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.vedtak.intern.AutomatiskFagsakAvslutningTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskFagsakAvslutningTjeneste {

    private ProsessTaskTjeneste taskTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;

    AutomatiskFagsakAvslutningTjeneste() {
        // For CDI?
    }

    @Inject
    public AutomatiskFagsakAvslutningTjeneste(ProsessTaskTjeneste taskTjeneste,
                                              FagsakRelasjonRepository fagsakRelasjonRepository) {
        this.taskTjeneste = taskTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
    }

    String avsluttFagsaker(String batchname, LocalDate date) {
        var fagsaker = fagsakRelasjonRepository.finnFagsakerForAvsluttning(date);

        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (var fagsak : fagsaker) {
            var nyCallId = callId + fagsak.getId();
            var task = opprettFagsakAvslutningTask(fagsak, nyCallId);
            taskTjeneste.lagre(task);
        }
        return batchname + "-" + UUID.randomUUID().toString();
    }

    private ProsessTaskData opprettFagsakAvslutningTask(Fagsak fagsak, String callId) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskFagsakAvslutningTask.class);
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setPrioritet(100);
        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);
        return prosessTaskData;
    }

}
