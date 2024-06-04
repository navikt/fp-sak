package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.avsluttBehandling", prioritet = 2)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvsluttBehandlingTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(AvsluttBehandlingTask.class);
    private AvsluttBehandling tjeneste;

    AvsluttBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttBehandlingTask(AvsluttBehandling tjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.tjeneste = tjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        tjeneste.avsluttBehandling(behandlingId);
        LOG.info("Utført for behandling: {}", behandlingId);
    }

}
