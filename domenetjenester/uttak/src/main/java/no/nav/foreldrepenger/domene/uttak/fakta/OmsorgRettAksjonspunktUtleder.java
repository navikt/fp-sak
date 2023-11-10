package no.nav.foreldrepenger.domene.uttak.fakta;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

public interface OmsorgRettAksjonspunktUtleder {

    List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input);

}
