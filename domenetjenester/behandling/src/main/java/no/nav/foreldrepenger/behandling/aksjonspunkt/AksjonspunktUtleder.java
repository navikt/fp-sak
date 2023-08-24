package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;

import java.util.List;

public interface AksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param);
}
