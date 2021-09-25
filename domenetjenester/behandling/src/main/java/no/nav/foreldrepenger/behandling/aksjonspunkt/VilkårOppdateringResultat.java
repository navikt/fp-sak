package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public class VilkårOppdateringResultat {

    private final VilkårType vilkårType;
    private final VilkårUtfallType vilkårUtfallType;
    private Avslagsårsak avslagsårsak;

    public VilkårOppdateringResultat(VilkårType vilkårType, VilkårUtfallType vilkårUtfallType) {
        this.vilkårType = vilkårType;
        this.vilkårUtfallType = vilkårUtfallType;
        this.avslagsårsak = Avslagsårsak.UDEFINERT;
    }

    public VilkårOppdateringResultat(VilkårType vilkårType, Avslagsårsak avslagsårsak) {
        this.vilkårType = vilkårType;
        this.vilkårUtfallType = VilkårUtfallType.IKKE_OPPFYLT;
        this.avslagsårsak = avslagsårsak;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    public VilkårUtfallType getVilkårUtfallType() {
        return vilkårUtfallType;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
