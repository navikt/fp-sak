package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public interface Overstyringshåndterer<T extends OverstyringAksjonspunkt> {

    OppdateringResultat håndterOverstyring(T dto, BehandlingReferanse ref);

    /**
     * Opprett Aksjonspunkt for Overstyring og håndter lagre historikk.
     */
    default void lagHistorikkInnslag(T dto, BehandlingReferanse ref) {

    }

    /**
     * Valider om precondition for overstyring er møtt. Kaster exception hvis ikke.
     */
    default void precondition(T dto, BehandlingReferanse ref) {
        // all good, do NOTHING.
    }
}
