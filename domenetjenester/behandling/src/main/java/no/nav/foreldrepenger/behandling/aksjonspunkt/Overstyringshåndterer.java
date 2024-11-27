package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public interface Overstyringshåndterer<T extends OverstyringAksjonspunkt> {

    OppdateringResultat håndterOverstyring(T dto, Behandling behandling, BehandlingskontrollKontekst kontekst);

    /**
     * Opprett Aksjonspunkt for Overstyring og håndter lagre historikk.
     */
    default void lagHistorikkInnslag(T dto, Behandling behandling) {

    }

    /**
     * Valider om precondition for overstyring er møtt. Kaster exception hvis ikke.
     */
    default void precondition(T dto, Behandling behandling) {
        // all good, do NOTHING.
    }
}
