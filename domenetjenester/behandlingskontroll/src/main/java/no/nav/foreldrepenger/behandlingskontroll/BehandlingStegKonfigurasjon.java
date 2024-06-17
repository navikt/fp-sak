package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;

/** For å få tak i riktig status konfigurasjon. */
public class BehandlingStegKonfigurasjon {

    private BehandlingStegKonfigurasjon() {
    }

    public static BehandlingStegStatus getStartet() {
        return BehandlingStegStatus.STARTET;
    }

    public static BehandlingStegStatus getInngang() {
        return BehandlingStegStatus.INNGANG;
    }

    public static BehandlingStegStatus getVenter() {
        return BehandlingStegStatus.VENTER;
    }

    public static BehandlingStegStatus getUtgang() {
        return BehandlingStegStatus.UTGANG;
    }

    public static BehandlingStegStatus getAvbrutt() {
        return BehandlingStegStatus.AVBRUTT;
    }

    public static BehandlingStegStatus getUtført() {
        return BehandlingStegStatus.UTFØRT;
    }

    public static BehandlingStegStatus getTilbakeført() {
        return BehandlingStegStatus.TILBAKEFØRT;
    }

    public static BehandlingStegStatus mapTilStatus(BehandlingStegResultat stegResultat) {
        return BehandlingStegResultat.mapTilStatus(stegResultat);

    }

}
