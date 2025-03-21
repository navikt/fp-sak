package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

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

import java.util.List;

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
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        // Eventuelt fortsettBehandling dersom registerdata ikke trengs hentes på nytt
        //prosesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, LocalDateTime.now());
        if (behandling.harUtførtAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON)) {
            var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VURDER_UTTAK_DOKUMENTASJON);
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON, List.of(ap));
        }
        prosesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }
}
