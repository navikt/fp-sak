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

    /**
     * Returnerer alle skjæringstidspunkter for avsluttet behandling med ytelse for revurdering-analyse-formål
     * FP: Skjæringstidspunkt for Foreldrepenger
     * 1. Utledet stp er utledet fra uttaket (ikke lagret opptjeningsperiode)
     * 2. Opptjening stp er som fastsatt/lagret
     * 3. Uttaksdatoer basert på innvilget uttak og etablerte fødselsdatoer
     * 3. Ønsket startdato for permijson (Uavklart)
     *
     * @param behandlingId
     */
    default Skjæringstidspunkt getSkjæringstidspunkterForAvsluttetBehandling(Long behandlingId) {
        return getSkjæringstidspunkter(behandlingId);
    }

}
