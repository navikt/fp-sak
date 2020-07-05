package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import static no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties.TASKTYPE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AvsluttOppgaveTask extends BehandlingProsessTask {
    private OppgaveTjeneste oppgaveTjeneste;

    AvsluttOppgaveTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttOppgaveTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        String oppgaveId = prosessTaskData.getOppgaveId()
            .orElseThrow(() -> new IllegalStateException("Mangler oppgaveId"));

        oppgaveTjeneste.avslutt(behandlingId, oppgaveId);
    }
}
