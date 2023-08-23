package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

import static java.util.Collections.singletonList;

@BehandlingStegRef(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private final VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste;

    @Inject
    public VurderSøknadsfristSteg(@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) VurderSøknadsfristTjeneste vurderSøknadsfristTjeneste) {
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();

        // Sjekk søknadsfrist for søknadsperioder
        var søknadfristAksjonspunktDefinisjon = vurderSøknadsfristTjeneste.vurder(behandlingId);

        // Returner eventuelt aksjonspunkt ifm søknadsfrist
        return søknadfristAksjonspunktDefinisjon
            .map(ad -> BehandleStegResultat.utførtMedAksjonspunkter(singletonList(ad)))
            .orElseGet(BehandleStegResultat::utførtUtenAksjonspunkter);
    }

}
