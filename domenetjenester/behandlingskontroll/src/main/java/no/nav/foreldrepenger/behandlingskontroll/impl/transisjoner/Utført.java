package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;

class Utført implements StegTransisjon {

    @Override
    public String getId() {
        return FellesTransisjoner.UTFØRT.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        return nåværendeSteg.getBehandlingModell().finnNesteSteg(nåværendeSteg.getBehandlingStegType());
    }

    @Override
    public String toString() {
        return "Utført{}";
    }
}
