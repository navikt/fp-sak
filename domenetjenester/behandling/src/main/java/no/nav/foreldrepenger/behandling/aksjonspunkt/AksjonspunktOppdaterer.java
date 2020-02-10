package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

/** Interface for å oppdatere aksjonspunkter. */
public interface AksjonspunktOppdaterer<T> {

    OppdateringResultat oppdater(T dto, AksjonspunktOppdaterParameter param);

    @SuppressWarnings("unused")
    default boolean skalReinnhenteRegisteropplysninger(Behandling behandling, LocalDate forrigeSkjæringstidspunkt) {
        return false;
    }

}
