package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;

public interface OpptjeningsVilkårTjeneste  {

    VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse);
}
