package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.time.LocalDate;

/** Interface for å oppdatere aksjonspunkter. */
public interface AksjonspunktOppdaterer<T> {

    OppdateringResultat oppdater(T dto, AksjonspunktOppdaterParameter param);

    @SuppressWarnings("unused")
    default boolean skalReinnhenteRegisteropplysninger(Long behandlingId, LocalDate forrigeSkjæringstidspunkt) {
        return false;
    }

}
