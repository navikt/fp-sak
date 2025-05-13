package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "migrering.tilbakeforbehandling", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class TilbakeføringTilStegTask extends BehandlingProsessTask {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;

    public TilbakeføringTilStegTask() {
        // For CDI
    }

    @Inject
    public TilbakeføringTilStegTask(BehandlingRepositoryProvider repositoryProvider,
                                    BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                    BehandlingProsesseringTjeneste prosesseringTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosesseringTjeneste = prosesseringTjeneste;
    }



    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling, lås);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        // fortsettBehandling dersom registerdata ikke trengs hentes på nytt, for nye registerdata bruk GjenopptaOppdaterFortsett
        //prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
        prosesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }
}
