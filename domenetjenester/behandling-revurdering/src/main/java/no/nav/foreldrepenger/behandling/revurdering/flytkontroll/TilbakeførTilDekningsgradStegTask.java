package no.nav.foreldrepenger.behandling.revurdering.flytkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("behandlingskontroll.tilbakeforDekningsgrad")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class TilbakeførTilDekningsgradStegTask extends FagsakProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(TilbakeførTilDekningsgradStegTask.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Inject
    public TilbakeførTilDekningsgradStegTask(BehandlingRepositoryProvider repositoryProvider,
                                             FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                             YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    TilbakeførTilDekningsgradStegTask() {
        // for CDI proxy
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow();
        behandlingRepository.taSkriveLås(behandling);
        if (behandling.erSaksbehandlingAvsluttet() || SpesialBehandling.erSpesialBehandling(behandling)) {
            return;
        }

        var fagsakrelDekningsgrad = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak())
            .map(FagsakRelasjon::getDekningsgrad);
        var behandlingDekningsgrad = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getGjeldendeDekningsgrad);
        if (fagsakrelDekningsgrad.isEmpty() || behandlingDekningsgrad.isEmpty()) {
            LOG.warn("Burde ikke ha opprettet task på sak {}. Fagsakrel dekningsgrad eller yfa dekningsgrad er {} {}.",
                behandling.getSaksnummer(), fagsakrelDekningsgrad, behandlingDekningsgrad);
            return;
        }
        if (fagsakrelDekningsgrad.get().equals(behandlingDekningsgrad.get())) {
            LOG.info("Fagsakrel dekningsgrad og yfa dekningsgrad er lik. Tilbakefører ikke behandling");
            return;
        }
        if (erIStegTidligereEnnDekningsgrad(behandling)) {
            LOG.info("Annen parts behandling ligger før dekningsgrad steget");
            return;
        }

        if (behandling.isBehandlingPåVent()) {
            behandlingProsesseringTjeneste.taBehandlingAvVent(behandling);
        }
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, BehandlingStegType.DEKNINGSGRAD);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    private boolean erIStegTidligereEnnDekningsgrad(Behandling behandling) {
        return behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.DEKNINGSGRAD);
    }
}
