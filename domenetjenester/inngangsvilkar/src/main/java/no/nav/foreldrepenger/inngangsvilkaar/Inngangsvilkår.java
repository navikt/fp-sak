package no.nav.foreldrepenger.inngangsvilkaar;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public interface Inngangsvilkår {

    /**
     * Vurder vilkår og returner utfall
     *
     * @param ref
     *            - med grunnlag som skal vurderes
     * @return {@link VilkårData} som beskriver utfall
     */
    VilkårData vurderVilkår(BehandlingReferanse ref);

}
