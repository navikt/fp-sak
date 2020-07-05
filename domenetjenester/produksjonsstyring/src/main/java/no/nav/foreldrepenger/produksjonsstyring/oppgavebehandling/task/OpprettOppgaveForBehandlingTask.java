package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import static no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingTask.TASKTYPE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveForBehandlingTask extends BehandlingProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveBehandleSak";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveForBehandlingTask.class);
    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveForBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveForBehandlingTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        String oppgaveId = oppgaveTjeneste.opprettBehandleOppgaveForBehandling(behandlingId);
        if (oppgaveId != null) {
            log.info("Oppgave opprettet i GSAK for å behandle sak. Oppgavenummer: {}", oppgaveId); //NOSONAR
        }
    }
}
