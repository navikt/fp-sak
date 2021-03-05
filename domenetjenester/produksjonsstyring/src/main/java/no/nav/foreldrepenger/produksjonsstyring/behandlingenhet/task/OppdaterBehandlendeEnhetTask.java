package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.task;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(OppdaterBehandlendeEnhetTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OppdaterBehandlendeEnhetTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "oppgavebehandling.oppdaterEnhet";

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
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<OrganisasjonsEnhet> nyEnhet = behandlendeEnhetTjeneste.sjekkOppdatertEnhetEtterReallokering(behandling);
        if (nyEnhet.isPresent()) {
            LOG.info("Endrer behandlende enhet for behandling: {}", prosessTaskData.getBehandlingId()); //NOSONAR
            behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, nyEnhet.get(), HistorikkAktør.VEDTAKSLØSNINGEN, BEGRUNNELSE);
        }
    }
}
