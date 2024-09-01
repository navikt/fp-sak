package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

public abstract class ForeslåBehandlingsresultatStegFelles implements ForeslåBehandlingsresultatSteg {

    private static final Logger LOG = LoggerFactory.getLogger(ForeslåBehandlingsresultatStegFelles.class);

    private BehandlingRepository behandlingRepository;
    private Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste;

    protected ForeslåBehandlingsresultatStegFelles() {
        // for CDI proxy
    }

    public ForeslåBehandlingsresultatStegFelles(BehandlingRepositoryProvider repositoryProvider,
            @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.foreslåBehandlingsresultatTjeneste = foreslåBehandlingsresultatTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        LOG.info("Foreslår behandlingsresultat for behandling {}", ref);

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(foreslåBehandlingsresultatTjeneste, ref.fagsakYtelseType()).orElseThrow();
        tjeneste.foreslåBehandlingsresultat(ref);

        // TODO (Safir/OSS): Lagre Behandlingsresultat gjennom eget repository
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        // Dette steget genererer ingen aksjonspunkter
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
