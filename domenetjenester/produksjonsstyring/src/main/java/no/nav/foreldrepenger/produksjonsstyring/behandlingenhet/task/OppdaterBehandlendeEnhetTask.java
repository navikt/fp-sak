package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
@ProsessTask(value = "oppgavebehandling.oppdaterEnhet", prioritet = 3)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterBehandlendeEnhetTask extends BehandlingProsessTask {

    public static final String BEGRUNNELSE = "Enhetsomlegging";

    private static final Logger LOG = LoggerFactory.getLogger(OppdaterBehandlendeEnhetTask.class);

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;

    OppdaterBehandlendeEnhetTask() {
        // for CDI proxy
    }

    @Inject
    public OppdaterBehandlendeEnhetTask(BehandlingRepositoryProvider repositoryProvider, BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlendeEnhetTjeneste.sjekkSkalOppdatereEnhet(behandling)
            .ifPresent(nyEnhet -> {
                LOG.info("Endrer behandlende enhet for behandling: {}", behandlingId);
                behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, nyEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, BEGRUNNELSE);
        });
    }
}
