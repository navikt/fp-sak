package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "migrering.tilbakeforadopsjon", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class AdopsjonTilbakeføringTask extends BehandlingProsessTask {

    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;

    public AdopsjonTilbakeføringTask() {
        // For CDI
    }

    @Inject
    public AdopsjonTilbakeføringTask(BehandlingRepositoryProvider repositoryProvider,
                                     BehandlingProsesseringTjeneste prosesseringTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.prosesseringTjeneste = prosesseringTjeneste;
    }



    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.isBehandlingPåVent()) {
            prosesseringTjeneste.taBehandlingAvVent(behandling);
        }
        prosesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, BehandlingStegType.KONTROLLER_FAKTA);
        if (behandling.isBehandlingPåVent()) {
            prosesseringTjeneste.taBehandlingAvVent(behandling);
        }
        // fortsettBehandling dersom registerdata ikke trengs hentes på nytt, for nye registerdata bruk GjenopptaOppdaterFortsett
        prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
    }
}
