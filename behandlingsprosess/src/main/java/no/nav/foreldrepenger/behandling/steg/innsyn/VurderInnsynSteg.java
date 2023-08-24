package no.nav.foreldrepenger.behandling.steg.innsyn;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import java.util.Collections;

@BehandlingStegRef(BehandlingStegType.VURDER_INNSYN)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderInnsynSteg implements BehandlingSteg {

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtMedAksjonspunkter(Collections.singletonList(AksjonspunktDefinisjon.VURDER_INNSYN));
    }
}
