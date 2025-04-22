package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;

/** For å få tak i riktig status konfigurasjon. */
public class BehandlingStegKonfigurasjon {

    private BehandlingStegKonfigurasjon() {
    }

    public static BehandlingStegStatus getUtført() {
        return BehandlingStegStatus.UTFØRT;
    }

    public static BehandlingStegStatus getTilbakeført() {
        return BehandlingStegStatus.TILBAKEFØRT;
    }

    public static BehandlingStegStatus getFramoverført() {
        return BehandlingStegStatus.FREMOVERFØRT;
    }

}
