package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;

/**
 * Brukes for intern håndtering av flyt på et steg. Inneholder kode for stegets
 * nye status. Hvis status er fremoverføring, er også steget det skal
 * fremoverføres til inkludert.
 */
public class StegProsesseringResultat {
    private final BehandlingStegStatus nyStegStatus;
    private final TransisjonIdentifikator transisjon;

    private StegProsesseringResultat(BehandlingStegStatus nyStegStatus, TransisjonIdentifikator transisjon) {
        this.nyStegStatus = nyStegStatus;
        this.transisjon = transisjon;
    }

    public static StegProsesseringResultat medMuligTransisjon(BehandlingStegStatus nyStegStatus, TransisjonIdentifikator transisjon) {
        return new StegProsesseringResultat(nyStegStatus, transisjon);
    }

    public static StegProsesseringResultat utenOverhopp(BehandlingStegStatus nyStegStatus) {
        return new StegProsesseringResultat(nyStegStatus, FellesTransisjoner.UTFØRT);
    }

    public TransisjonIdentifikator getTransisjon() {
        return transisjon;
    }

    public BehandlingStegStatus getNyStegStatus() {
        return nyStegStatus;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<nyStegStatus=" + nyStegStatus + ", transisjon=" + transisjon + ">";
    }

}
