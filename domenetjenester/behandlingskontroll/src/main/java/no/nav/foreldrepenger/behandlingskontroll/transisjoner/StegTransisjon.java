package no.nav.foreldrepenger.behandlingskontroll.transisjoner;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

import java.util.Optional;

public interface StegTransisjon {
    String getId();

    BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg);

    default Optional<BehandlingStegType> getMålstegHvisHopp() {
        return Optional.empty();
    }

    default BehandlingStegResultat getRetningForHopp() {
        throw new IllegalArgumentException("Utviklerfeil: skal ikke kalles for transisjon " + getId());
    }
}
