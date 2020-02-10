package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public interface BehandlingSteg {

    /**
     * Returner statuskode med ev nye aksjonspunkter funnet i dette steget.
     */
    BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst);

    default BehandleStegResultat gjenopptaSteg(@SuppressWarnings("unused") BehandlingskontrollKontekst kontekst) {
        throw new IllegalStateException("Utviklerfeil: gjenopptaSteg skal ikke kalles uten implementasjon");
    }

    /**
     * Template Method - transisjoner utover normal flyt (utførSteg) for opprydding el.
     * @param kontekst
     *            - overordnet kontekst informasjon og lås for å gjøre endringer på behandlingen.
     * @param modell
*            - BehandlingStegModell som kan benyttes til oppslag av hvordan flyten skal være
     * @param førsteSteg
*       - Det første steget av stegene det hoppes mellom. Vil være steget det hoppes til ved bakoverhopp.
     * @param sisteSteg
*       - Det siste steget av stegene det hoppes mellom. Vil være steget det hoppes fra ved bakoverhopp.
     */
    default void vedTransisjon(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, TransisjonType transisjonType,
                               BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        switch (transisjonType) {
            case HOPP_OVER_BAKOVER:
                vedHoppOverBakover(kontekst, modell, førsteSteg, sisteSteg);
                break;
            case HOPP_OVER_FRAMOVER:
                vedHoppOverFramover(kontekst, modell, førsteSteg, sisteSteg);
                break;
            default:
                throw new IllegalArgumentException("Uhåndtert transisjonType: " + transisjonType //$NON-NLS-1$
                        + " i steg: " + modell.getBehandlingStegType()); //$NON-NLS-1$
        }
    }

    @SuppressWarnings("unused")
    default void vedInngang(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell) {
        // TEMPLATE METHOD
    }

    @SuppressWarnings("unused")
    default void vedUtgang(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell) {
        // TEMPLATE METHOD
    }

    @SuppressWarnings("unused")
    default void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        // TEMPLATE METHOD
    }

    @SuppressWarnings("unused")
    default void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        // TEMPLATE METHOD
    }

     enum TransisjonType {
        /**
         * Kalles for steg som hoppes over når behandlingen skrider frem. Kan f.eks. brukes til å avbryte
         *
         * resultater/vilkår som ikke kan settes når et steg hoppes over. Aksjonspunkt vil håndteres automatisk avhengig
         * av hvilket vurderingspunkt de tilhører og hvor de ble identifisert.
         */
        HOPP_OVER_FRAMOVER,

        /**
         * Kalles for steg som hoppes over ved tilbakeføring av behandling. Kan f.eks. brukes til å rydde vekk
         *
         * resultater/vilkår som bør resettes. Aksjonspunkt vil håndteres automatisk avhengig av hvilket vurderingspunkt
         * de tilhører.
         */
        HOPP_OVER_BAKOVER
    }
}
