package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;


import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("oppgavebehandling.avsluttOppgave")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AvsluttOppgaveTask extends GenerellProsessTask {

    private OppgaveTjeneste oppgaveTjeneste;

    AvsluttOppgaveTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttOppgaveTask(OppgaveTjeneste oppgaveTjeneste) {
        super();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var oppgaveId = OppgaveTjeneste.getOppgaveId(prosessTaskData)
            .orElseThrow(() -> new IllegalStateException("Mangler oppgaveId"));

        oppgaveTjeneste.avslutt(behandlingId, oppgaveId);
    }
}
