package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "migrering.migrertilomsorgrett", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MigrerTilOmsorgRettTask extends BehandlingProsessTask {

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingsprosessTjeneste prosesseringTjeneste;

    public MigrerTilOmsorgRettTask() {
        // For CDI
    }

    @Inject
    public MigrerTilOmsorgRettTask(BehandlingRepositoryProvider repositoryProvider,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   BehandlingsprosessTjeneste prosesseringTjeneste) {
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
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.VURDER_OPPTJENING_FAKTA);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        prosesseringTjeneste.asynkKjørProsess(behandling);
    }
}
