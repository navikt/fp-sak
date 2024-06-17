package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import java.time.LocalDateTime;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Oppretter videre tasks for gjenoppta og oppdater registerdata
 */
@Dependent
@ProsessTask(value = "batch.opprett.gjenoppta", prioritet = 3)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettGjenopptaTask extends GenerellProsessTask {

    private final BehandlingRepository behandlingRepository;
    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Inject
    public OpprettGjenopptaTask(BehandlingRepository behandlingRepository, BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        super();
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandlingReadOnly(behandlingId);
        behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsettBatch(behandling, LocalDateTime.now());
    }

}
