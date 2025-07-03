package no.nav.foreldrepenger.mottak.sakskompleks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "kompletthettjeneste.gjenopptaBehandling", prioritet = 2, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class GjenopptaKøetBehandlingTask extends FagsakProsessTask {

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
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        if (prosessTaskData.getBehandlingIdAsLong() != null) {
            køKontroller.oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(prosessTaskData.getBehandlingIdAsLong());
        }
    }
}
