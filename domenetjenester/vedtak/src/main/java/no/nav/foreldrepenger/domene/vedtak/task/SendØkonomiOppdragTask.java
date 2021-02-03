package no.nav.foreldrepenger.domene.vedtak.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.økonomistøtte.ØkonomiOppdragKøTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(SendØkonomiOppdragTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendØkonomiOppdragTask extends GenerellProsessTask {
    public static final String TASKTYPE = "iverksetteVedtak.sendØkonomiOppdrag";

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
