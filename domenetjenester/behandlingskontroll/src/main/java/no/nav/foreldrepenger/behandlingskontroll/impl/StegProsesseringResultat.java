package no.nav.foreldrepenger.behandlingskontroll.impl;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.Transisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;

/**
 * Brukes for intern håndtering av flyt på et steg. Inneholder kode for stegets
 * nye status. Hvis status er fremoverføring, er også steget det skal
 * fremoverføres til inkludert.
 */
public class StegProsesseringResultat {
    private final BehandlingStegStatus nyStegStatus;
    private final Transisjon transisjon;

    private StegProsesseringResultat(BehandlingStegStatus nyStegStatus, Transisjon transisjon) {
        this.nyStegStatus = nyStegStatus;
        this.transisjon = transisjon;
    }

    public static StegProsesseringResultat medMuligTransisjon(BehandlingStegStatus nyStegStatus, Transisjon transisjon) {
        return new StegProsesseringResultat(nyStegStatus, transisjon);
    }

    public static StegProsesseringResultat medMuligTransisjon(BehandlingStegStatus nyStegStatus, StegTransisjon transisjon) {
        return new StegProsesseringResultat(nyStegStatus, new Transisjon(transisjon, null));
    }

    public static StegProsesseringResultat utenOverhopp(BehandlingStegStatus nyStegStatus) {
        return new StegProsesseringResultat(nyStegStatus, new Transisjon(StegTransisjon.UTFØRT, null));
    }

    public Transisjon getTransisjon() {
        return transisjon;
    }

    public BehandlingStegStatus getNyStegStatus() {
        return nyStegStatus;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<nyStegStatus=" + nyStegStatus + ", stegTransisjon=" + transisjon + ">";
    }

}
