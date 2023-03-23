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
@ProsessTask("oppgavebehandling.kontrollenhet")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterBehandlendeEnhetKontrollTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterBehandlendeEnhetKontrollTask.class);

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;

    OppdaterBehandlendeEnhetKontrollTask() {
        // for CDI proxy
    }

    @Inject
    public OppdaterBehandlendeEnhetKontrollTask(BehandlingRepositoryProvider repositoryProvider, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlendeEnhetTjeneste.erKontrollEnhet(behandling)) {
            return;
        }
        LOG.info("Endrer behandlende enhet til kontroll for behandling: {}", prosessTaskData.getBehandlingId());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhetKontroll(behandling, HistorikkAktør.VEDTAKSLØSNINGEN, "Endret saksmarkering");

    }
}
