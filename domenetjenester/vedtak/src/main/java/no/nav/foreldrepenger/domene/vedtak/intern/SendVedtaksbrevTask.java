package no.nav.foreldrepenger.domene.vedtak.intern;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@ProsessTask("iverksetteVedtak.sendVedtaksbrev")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SendVedtaksbrevTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(SendVedtaksbrevTask.class);

    private SendVedtaksbrev tjeneste;

    SendVedtaksbrevTask() {
        // for CDI proxy
    }

    @Inject
    public SendVedtaksbrevTask(SendVedtaksbrev tjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.tjeneste = tjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        tjeneste.sendVedtaksbrev(behandlingId);
        LOG.info("Utført for behandling: {}", behandlingId);
    }
}
