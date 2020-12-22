package no.nav.foreldrepenger.behandling.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public class VilkårOppdateringResultat {

    private final VilkårType vilkårType;
    private final VilkårUtfallType vilkårUtfallType;
    private VilkårUtfallMerknad vilkårUtfallMerknad;
    private Avslagsårsak avslagsårsak;

    public VilkårOppdateringResultat(VilkårType vilkårType, VilkårUtfallType vilkårUtfallType) {
        this.vilkårType = vilkårType;
        this.vilkårUtfallType = vilkårUtfallType;
    }

    public VilkårOppdateringResultat(VilkårType vilkårType, Avslagsårsak avslagsårsak) {
        this.vilkårType = vilkårType;
        this.vilkårUtfallType = VilkårUtfallType.IKKE_OPPFYLT;
        this.avslagsårsak = avslagsårsak;
    }

    public VilkårOppdateringResultat(VilkårType vilkårType,
                                     Avslagsårsak avslagsårsak,
                                     VilkårUtfallMerknad merknad) {
        this.vilkårType = vilkårType;
        this.vilkårUtfallType = VilkårUtfallType.IKKE_OPPFYLT;
        this.avslagsårsak = avslagsårsak;
        this.vilkårUtfallMerknad = merknad;
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

    public VilkårUtfallMerknad getVilkårUtfallMerknad() {
        return vilkårUtfallMerknad;
    }
}
