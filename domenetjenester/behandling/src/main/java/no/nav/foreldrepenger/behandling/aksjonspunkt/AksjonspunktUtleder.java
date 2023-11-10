package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;

public interface AksjonspunktUtleder {

    List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param);
}
