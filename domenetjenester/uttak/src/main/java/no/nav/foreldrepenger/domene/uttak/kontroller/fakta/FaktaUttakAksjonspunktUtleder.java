package no.nav.foreldrepenger.domene.uttak.kontroller.fakta;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

public interface FaktaUttakAksjonspunktUtleder {

    List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input);

    boolean skalBrukesVedOppdateringAvYtelseFordeling();
}
