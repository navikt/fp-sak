package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;

class SettPåVent implements StegTransisjon {

    @Override
    public String getId() {
        return FellesTransisjoner.SETT_PÅ_VENT.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        return nåværendeSteg;
    }

    @Override
    public String toString() {
        return "SettPåVent{}";
    }
}
