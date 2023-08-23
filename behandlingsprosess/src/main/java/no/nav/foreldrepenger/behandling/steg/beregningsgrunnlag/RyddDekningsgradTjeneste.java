package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class RyddDekningsgradTjeneste {

    private BehandlingLåsRepository behandlingLåsRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    RyddDekningsgradTjeneste() {
        // CDI
    }

    @Inject
    public RyddDekningsgradTjeneste(BehandlingLåsRepository behandlingLåsRepository,
            BehandlingRepository behandlingRepository,
            FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
            BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    public void rydd(Behandling behandling) {
        ryddIBehandlingsresultat(behandling);
        ryddIFagsakRelasjon(behandling);
    }

    private void ryddIFagsakRelasjon(Behandling behandling) {
        fagsakRelasjonTjeneste.nullstillOverstyrtDekningsgrad(behandling.getFagsak());
    }

    private void ryddIBehandlingsresultat(Behandling behandling) {
        var resultat = behandlingsresultatRepository.hent(behandling.getId());
        Behandlingsresultat.builderEndreEksisterende(resultat).medEndretDekningsgrad(false).buildFor(behandling);
        behandlingRepository.lagre(behandling, behandlingLåsRepository.taLås(behandling.getId()));
    }
}
