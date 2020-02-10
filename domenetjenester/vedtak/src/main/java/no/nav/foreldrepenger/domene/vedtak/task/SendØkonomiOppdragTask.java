package no.nav.foreldrepenger.domene.vedtak.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomiOppdragKøTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(SendØkonomiOppdragTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class SendØkonomiOppdragTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "iverksetteVedtak.sendØkonomiOppdrag";

    private ØkonomiOppdragKøTjeneste køTjeneste;

    public SendØkonomiOppdragTask() {
        // CDI krav
    }

    @Inject
    public SendØkonomiOppdragTask(ØkonomiOppdragKøTjeneste køTjeneste) {
        this.køTjeneste = køTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        køTjeneste.leggOppdragPåKø(prosessTaskData.getBehandlingId());
    }
}
