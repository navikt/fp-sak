package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

import java.util.List;

public interface InngangsvilkårSteg extends BehandlingSteg {

    /** Vilkår håndtert (vurdert) i dette steget. */
    List<VilkårType> vilkårHåndtertAvSteg();
}
