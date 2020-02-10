package no.nav.foreldrepenger.skjæringstidspunkt;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface SkjæringstidspunktTjeneste {

    /**
     * Returnerer alle skjæringstidspunkter for
     * ES: Skjæringstidspunkt for Engangsstønad(hendelses skjæringstidspunkt)
     * FP: Skjæringstidspunkt for Foreldrepenger
     * 1. Opptjeningsperiode tom-dato (Bekreftet)
     * 2. Avklart startdato for permisjon (Delevis avklart)
     * 3. Ønsket startdato for permijson (Uavklart)
     * 
     * @param behandlingId
     */
    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId);

}
