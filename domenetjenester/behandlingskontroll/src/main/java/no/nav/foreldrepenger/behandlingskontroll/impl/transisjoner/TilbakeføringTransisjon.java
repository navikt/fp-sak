package no.nav.foreldrepenger.behandlingskontroll.impl.transisjoner;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;

class TilbakeføringTransisjon implements StegTransisjon {

    @Override
    public String getId() {
        return FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT.getId();
    }

    @Override
    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        throw new IllegalArgumentException("Utvikler-feil: skal ikke kalle nesteSteg på " + getId());
    }

    @Override
    public String toString() {
        return "TilbakeføringTransisjon";
    }
}
