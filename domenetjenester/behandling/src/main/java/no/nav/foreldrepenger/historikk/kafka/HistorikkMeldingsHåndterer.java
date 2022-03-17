package no.nav.foreldrepenger.historikk.kafka;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ActivateRequestContext
@Transactional
public class HistorikkMeldingsHåndterer {
    private ProsessTaskTjeneste taskTjeneste;

    public HistorikkMeldingsHåndterer() {
    }

    @Inject
    public HistorikkMeldingsHåndterer(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    void lagreMelding(@SuppressWarnings("unused") String header, String payload) { // NOSONAR
        var data = ProsessTaskData.forProsessTask(LagreJournalpostTask.class);
        data.setPayload(payload);
        taskTjeneste.lagre(data);
    }
}
