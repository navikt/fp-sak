package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Optional;
import java.util.Properties;

public class VilkårBuilder {
    private Vilkår kladd;
    private boolean oppdatering;

    private VilkårBuilder(Vilkår vilkår, boolean oppdatering) {
        this.kladd = vilkår;
        this.oppdatering = oppdatering;
    }

    static VilkårBuilder ny() {
        return new VilkårBuilder(new Vilkår(), false);
    }

    static VilkårBuilder oppdatere(Vilkår vilkår) {
        return new VilkårBuilder(vilkår, true);
    }

    public static VilkårBuilder oppdatere(Optional<Vilkår> vilkår) {
        return vilkår.map(VilkårBuilder::oppdatere).orElseGet(VilkårBuilder::ny);
    }

    public VilkårBuilder medVilkårType(VilkårType vilkårType) {
        if (kladd.getVilkårType() != null && !vilkårType.equals(kladd.getVilkårType())) {
            throw new IllegalArgumentException("Prøver endre vilkårtype på eksisterende vilkår");
        }
        kladd.setVilkårType(vilkårType);
        return this;
    }

    public VilkårBuilder medVilkårUtfall(VilkårUtfallType vilkårUtfall, Avslagsårsak avslagsårsak) {
        if (VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårUtfall) && (avslagsårsak == null || Avslagsårsak.UDEFINERT.equals(avslagsårsak)))
            throw new IllegalArgumentException("Mangler avslagsårsak");
        kladd.setVilkårUtfall(vilkårUtfall);
        if (!VilkårUtfallType.IKKE_OPPFYLT.equals(kladd.getVilkårUtfallOverstyrt())) {
            kladd.setAvslagsårsak(avslagsårsak);
        }
        return this;
    }

    public VilkårBuilder medUtfallManuell(VilkårUtfallType vilkårUtfallManuell, Avslagsårsak avslagsårsak) {
        if (VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårUtfallManuell) && (avslagsårsak == null || Avslagsårsak.UDEFINERT.equals(avslagsårsak)))
            throw new IllegalArgumentException("Mangler avslagsårsak");
        kladd.setVilkårUtfallManuelt(vilkårUtfallManuell);
        if (!VilkårUtfallType.IKKE_OPPFYLT.equals(kladd.getVilkårUtfallOverstyrt())) {
            kladd.setAvslagsårsak(avslagsårsak);
        }
        return this;
    }

    VilkårBuilder medUtfallOverstyrt(VilkårUtfallType vilkårUtfallOverstyrt, Avslagsårsak avslagsårsak) {
        if (VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårUtfallOverstyrt) && (avslagsårsak == null || Avslagsårsak.UDEFINERT.equals(avslagsårsak)))
            throw new IllegalArgumentException("Mangler avslagsårsak");
        kladd.setVilkårUtfallOverstyrt(vilkårUtfallOverstyrt);
        kladd.setAvslagsårsak(avslagsårsak);
        return this;
    }

    public VilkårBuilder medVilkårUtfallMerknad(VilkårUtfallMerknad vilkårUtfallMerknad) {
        kladd.setVilkårUtfallMerknad(vilkårUtfallMerknad != null ? vilkårUtfallMerknad : VilkårUtfallMerknad.UDEFINERT);
        return this;
    }

    public VilkårBuilder medMerknadParametere(Properties merknadParametere) {
        if (merknadParametere != null && !merknadParametere.isEmpty())
            kladd.setMerknadParametere(merknadParametere);
        return this;
    }

    public VilkårBuilder medRegelEvaluering(String regelEvaluering) {
        kladd.setRegelEvaluering(regelEvaluering);
        return this;
    }

    public VilkårBuilder medRegelInput(String regelInput) {
        kladd.setRegelInput(regelInput);
        return this;
    }

    Vilkår build() {
        if (VilkårType.UDEFINERT.equals(kladd.getVilkårType()) || VilkårUtfallType.UDEFINERT.equals(kladd.getGjeldendeVilkårUtfall())) {
            throw new IllegalStateException("Mangler vilkårType");
        }
        return kladd;
    }

    boolean erOppdatering() {
        return oppdatering;
    }

}
