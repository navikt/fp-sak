package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@Dependent
@ProsessTask("oppgavebehandling.opprettOppgaveGodkjennVedtak")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveGodkjennVedtakTask extends GenerellProsessTask {


    public OpprettOppgaveGodkjennVedtakTask() {
        super();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // NOOP
    }
}
