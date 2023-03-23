package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("oppgavebehandling.utlandenhet")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterBehandlendeEnhetUtlandTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterBehandlendeEnhetUtlandTask.class);

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;

    OppdaterBehandlendeEnhetUtlandTask() {
        // for CDI proxy
    }

    @Inject
    public OppdaterBehandlendeEnhetUtlandTask(BehandlingRepositoryProvider repositoryProvider, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlendeEnhetTjeneste.erUtlandsEnhet(behandling)) {
            return;
        }
        LOG.info("Endrer behandlende enhet til utland for behandling: {}", prosessTaskData.getBehandlingId());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhetUtland(behandling, HistorikkAktør.VEDTAKSLØSNINGEN, "Endret saksmarkering");

    }
}
