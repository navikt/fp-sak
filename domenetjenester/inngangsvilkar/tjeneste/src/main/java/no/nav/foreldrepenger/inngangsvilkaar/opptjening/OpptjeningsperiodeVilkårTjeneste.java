package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;

public interface OpptjeningsperiodeVilkårTjeneste {

    // Takler behandlingreferanse som ikke har satt skjæringstidspunkt
    VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, LocalDate førsteUttaksdato);
}
