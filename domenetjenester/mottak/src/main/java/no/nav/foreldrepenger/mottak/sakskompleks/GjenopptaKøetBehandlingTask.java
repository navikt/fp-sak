package no.nav.foreldrepenger.mottak.sakskompleks;

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
@ProsessTask(GjenopptaKøetBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class GjenopptaKøetBehandlingTask extends FagsakProsessTask {

    public static final String TASKTYPE = "kompletthettjeneste.gjenopptaBehandling";

    private static final Logger LOG = LoggerFactory.getLogger(GjenopptaKøetBehandlingTask.class);

    private KøKontroller køKontroller;

    GjenopptaKøetBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public GjenopptaKøetBehandlingTask(KøKontroller køKontroller, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.køKontroller = køKontroller;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        if (behandlingId != null) {
            køKontroller.oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(behandlingId);
        }
    }
}
