package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "FORBRES")
@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-004")
@ApplicationScoped
public class ForeslåBehandlingsresultatStegRevurdering extends ForeslåBehandlingsresultatStegFelles {

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    ForeslåBehandlingsresultatStegRevurdering() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBehandlingsresultatStegRevurdering(BehandlingRepositoryProvider repositoryProvider,
                                                     @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste,
                                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, foreslåBehandlingsresultatTjeneste, skjæringstidspunktTjeneste);
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        behandlingsresultat.ifPresent(behandlingsresultat1 -> Behandlingsresultat.builderEndreEksisterende(behandlingsresultat1)
            .fjernKonsekvenserForYtelsen()
            .buildFor(behandling));
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }
}
