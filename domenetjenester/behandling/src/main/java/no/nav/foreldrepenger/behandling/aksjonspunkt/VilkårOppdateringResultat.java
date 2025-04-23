package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public record VilkårOppdateringResultat(VilkårType vilkårType, VilkårUtfallType vilkårUtfallType, Avslagsårsak avslagsårsak) {

    public VilkårOppdateringResultat(VilkårType vilkårType, VilkårUtfallType vilkårUtfallType) {
        this(vilkårType, vilkårUtfallType, Avslagsårsak.UDEFINERT);
    }

    public VilkårOppdateringResultat(VilkårType vilkårType, Avslagsårsak avslagsårsak) {
        this(vilkårType, VilkårUtfallType.IKKE_OPPFYLT, avslagsårsak);
    }

    public VilkårType getVilkårType() {
        return vilkårType();
    }

    public VilkårUtfallType getVilkårUtfallType() {
        return vilkårUtfallType();
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak();
    }
}
