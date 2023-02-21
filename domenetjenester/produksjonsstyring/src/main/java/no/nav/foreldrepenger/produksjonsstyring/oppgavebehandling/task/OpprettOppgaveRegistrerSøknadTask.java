package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import javax.enterprise.context.Dependent;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@Dependent
@ProsessTask("oppgavebehandling.opprettOppgaveRegistrerSøknad")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveRegistrerSøknadTask extends GenerellProsessTask {

    public OpprettOppgaveRegistrerSøknadTask() {
        super();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        // NOOP
    }
}
