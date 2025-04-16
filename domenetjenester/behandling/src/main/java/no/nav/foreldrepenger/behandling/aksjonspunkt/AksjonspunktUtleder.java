package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.List;

public interface AksjonspunktUtleder {

    List<AksjonspunktUtlederResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param);
}
