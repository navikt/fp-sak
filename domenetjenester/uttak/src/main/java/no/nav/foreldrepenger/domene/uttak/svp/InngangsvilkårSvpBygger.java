package no.nav.foreldrepenger.domene.uttak.svp;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.svangerskapspenger.regler.fastsettperiode.grunnlag.Inngangsvilkår;

public final class InngangsvilkårSvpBygger {

    private InngangsvilkårSvpBygger() {
    }

    public static Inngangsvilkår byggInngangsvilårSvp(VilkårResultat vilkårResultat) {
        return new Inngangsvilkår(vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.OPPTJENINGSVILKÅRET),
            vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.SVANGERSKAPSPENGERVILKÅR),
            vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.MEDLEMSKAPSVILKÅRET));
    }

    private static boolean vilkårAvTypeErOppfylt(VilkårResultat vilkårResultat, VilkårType type) {
        var vilkår = vilkårResultat.getVilkårene()
            .stream()
            .filter(v -> Objects.equals(v.getVilkårType(), type))
            .findFirst();
        return vilkår.map(v -> !v.erIkkeOppfylt()).orElse(true);
    }
}
