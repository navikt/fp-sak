package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task;

import java.util.Optional;

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

    public static final String BESTILLER_KEY = "bestiller";

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
        var aktør = Optional.ofNullable(prosessTaskData.getPropertyValue(BESTILLER_KEY)).map(HistorikkAktør::valueOf).orElse(HistorikkAktør.VEDTAKSLØSNINGEN);
        LOG.info("Endrer behandlende enhet til kontroll for behandling: {}", prosessTaskData.getBehandlingId());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhetKontroll(behandling, aktør, "Endret saksmarkering");

    }
}
