package no.nav.foreldrepenger.domene.vedtak.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.økonomistøtte.ØkonomiOppdragKøTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.sendØkonomiOppdrag", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendØkonomiOppdragTask extends GenerellProsessTask {

    private ØkonomiOppdragKøTjeneste køTjeneste;

    public SendØkonomiOppdragTask() {
        // CDI krav
    }

    @Inject
    public SendØkonomiOppdragTask(ØkonomiOppdragKøTjeneste køTjeneste) {
        super();
        this.køTjeneste = køTjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        køTjeneste.leggOppdragPåKø(behandlingId);
    }

}
