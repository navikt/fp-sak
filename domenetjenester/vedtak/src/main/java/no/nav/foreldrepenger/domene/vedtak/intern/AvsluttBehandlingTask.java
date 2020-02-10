package no.nav.foreldrepenger.domene.vedtak.intern;

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
@ProsessTask(AvsluttBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvsluttBehandlingTask extends FagsakProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.avsluttBehandling";
    private static final Logger log = LoggerFactory.getLogger(AvsluttBehandlingTask.class);
    private AvsluttBehandling tjeneste;

    AvsluttBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttBehandlingTask(AvsluttBehandling tjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.tjeneste = tjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingId = prosessTaskData.getBehandlingId();
        tjeneste.avsluttBehandling(behandlingId);
        log.info("Utført for behandling: {}", behandlingId);
    }
}
