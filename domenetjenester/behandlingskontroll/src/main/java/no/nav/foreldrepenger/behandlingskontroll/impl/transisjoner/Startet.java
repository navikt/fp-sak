package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;

class Startet implements StegTransisjon {
    @Override
    public String getId() {
        return FellesTransisjoner.STARTET.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        throw new IllegalArgumentException("Utvikler feil: skal ikke kalles for " + getId());
    }

    @Override
    public String toString() {
        return "Startet{}";
    }
}
