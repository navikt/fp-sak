package no.nav.foreldrepenger.behandlingskontroll.transisjoner;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public interface StegTransisjon {
    String getId();

    BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg);

    default Optional<BehandlingStegType> getMålstegHvisFremoverhopp() {
        return Optional.empty();
    }
}
