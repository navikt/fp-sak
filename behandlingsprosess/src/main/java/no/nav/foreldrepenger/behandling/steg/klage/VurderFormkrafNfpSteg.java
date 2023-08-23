package no.nav.foreldrepenger.behandling.steg.klage;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

import static java.util.Collections.singletonList;

@BehandlingStegRef(BehandlingStegType.KLAGE_VURDER_FORMKRAV_NFP)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderFormkrafNfpSteg implements BehandlingSteg {

    public VurderFormkrafNfpSteg() {
        // For CDI proxy
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var aksjonspunktDefinisjons = singletonList(AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP);
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjons);
    }
}
