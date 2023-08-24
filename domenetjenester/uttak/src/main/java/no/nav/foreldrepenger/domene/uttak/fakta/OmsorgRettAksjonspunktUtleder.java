package no.nav.foreldrepenger.domene.uttak.fakta;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

import java.util.List;

public interface OmsorgRettAksjonspunktUtleder {

    List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input);

}
