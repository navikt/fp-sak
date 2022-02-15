package no.nav.foreldrepenger.domene.oppdateringresultat;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;

public class BeregningVilkårResultat {

    private Boolean vilkårOppfylt;
    private Avslagsårsak avslagsårsak;

    public BeregningVilkårResultat(Boolean vilkårOppfylt, Avslagsårsak avslagsårsak) {
        this.vilkårOppfylt = vilkårOppfylt;
        this.avslagsårsak = avslagsårsak;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
