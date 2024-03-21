package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.vedtak.intern.AutomatiskFagsakAvslutningTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskFagsakAvslutningTjeneste {

    private ProsessTaskTjeneste taskTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    AutomatiskFagsakAvslutningTjeneste() {
        // For CDI?
    }

    @Inject
    public AutomatiskFagsakAvslutningTjeneste(ProsessTaskTjeneste taskTjeneste,
                                              FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
    }

    String avsluttFagsaker(String batchname, LocalDate date) {
        var fagsaker = fagsakRelasjonTjeneste.finnFagsakerForAvsluttning(date);

        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        var dato = LocalDate.now();
        var baseline = LocalTime.now();

        for (var fagsak : fagsaker) {
            var nyCallId = callId + fagsak.getId();
            var task = opprettFagsakAvslutningTask(fagsak, nyCallId, dato, baseline, 10799); // Spre kjøring utover 3 timer
            taskTjeneste.lagre(task);
        }
        return batchname + "-" + UUID.randomUUID();
    }

    private ProsessTaskData opprettFagsakAvslutningTask(Fagsak fagsak, String callId, LocalDate dato, LocalTime tid, int spread) {
        var nesteKjøring = LocalDateTime.of(dato, tid.plusSeconds(Math.abs(System.nanoTime()) % spread));
        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskFagsakAvslutningTask.class);
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setNesteKjøringEtter(nesteKjøring);
        prosessTaskData.setPrioritet(100);
        // unik per task da det er ulike tasks for hver behandling
        prosessTaskData.setCallId(callId);
        return prosessTaskData;
    }

}
