package no.nav.foreldrepenger.domene.vedtak.batch;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;

/**
 * Henter ut løpende Fagsaker og avslutter dem hvis det ikke er noen åpne behandlinger
 * og alle perioden for ytelsesvedtaket er passert
 *
 * Skal kjøre en gang i døgnet
 *
 * Kan kjøres med parameter date=<Datoen man vil batchen skal kjøres for(på format dd-MM-yyyy)>
 */

@ApplicationScoped
public class AutomatiskFagsakAvslutningBatchTjeneste implements BatchTjeneste {

    private static final String BATCHNAME = "BVL006";
    private AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste;

    @Inject
    public AutomatiskFagsakAvslutningBatchTjeneste(AutomatiskFagsakAvslutningTjeneste automatiskFagsakAvslutningTjeneste) {
        this.automatiskFagsakAvslutningTjeneste = automatiskFagsakAvslutningTjeneste;
    }

    @Override
    public String launch(BatchArguments arguments) {
        final var avsluttFagsakGruppe = automatiskFagsakAvslutningTjeneste.avsluttFagsaker(BATCHNAME, LocalDate.now());
        return BATCHNAME + "-" + (avsluttFagsakGruppe != null ? avsluttFagsakGruppe : UUID.randomUUID().toString());
    }

    @Override
    public BatchStatus status(String batchInstanceNumber) {
        final var gruppe = batchInstanceNumber.substring(batchInstanceNumber.indexOf('-') + 1);
        final var taskStatuses = automatiskFagsakAvslutningTjeneste.hentStatusForFagsakAvslutningGruppe(gruppe);

        if (isCompleted(taskStatuses)) {
            if (isContainingFailures(taskStatuses)) {
                return BatchStatus.WARNING;
            }
            return BatchStatus.OK;
        }
        // Is still running
        return BatchStatus.RUNNING;
    }

    private boolean isContainingFailures(List<TaskStatus> taskStatuses) {
        return taskStatuses.stream().anyMatch(it -> it.getStatus() == ProsessTaskStatus.FEILET);
    }

    private boolean isCompleted(List<TaskStatus> taskStatuses) {
        return taskStatuses.isEmpty() || taskStatuses.stream().noneMatch(it -> it.getStatus() == ProsessTaskStatus.KLAR);
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

}

