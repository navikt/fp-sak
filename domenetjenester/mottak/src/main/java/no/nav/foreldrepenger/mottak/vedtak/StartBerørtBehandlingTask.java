package no.nav.foreldrepenger.mottak.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(StartBerørtBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class StartBerørtBehandlingTask extends FagsakProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.startBerørtBehandling";
    private static final Logger log = LoggerFactory.getLogger(StartBerørtBehandlingTask.class);
    private BerørtBehandlingKontroller tjeneste;

    StartBerørtBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public StartBerørtBehandlingTask(BerørtBehandlingKontroller tjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.tjeneste = tjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {

        tjeneste.vurderNesteOppgaveIBehandlingskø(behandlingId);
        log.info("Utført for fagsak: {}", fagsakId);
    }
}
