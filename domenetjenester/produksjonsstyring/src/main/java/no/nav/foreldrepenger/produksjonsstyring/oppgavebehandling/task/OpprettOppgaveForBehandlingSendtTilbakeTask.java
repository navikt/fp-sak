package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@Dependent
@ProsessTask("oppgavebehandling.opprettOppgaveSakSendtTilbake")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveForBehandlingSendtTilbakeTask extends GenerellProsessTask {


    public OpprettOppgaveForBehandlingSendtTilbakeTask() {
        super();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // NOOP
    }
}
