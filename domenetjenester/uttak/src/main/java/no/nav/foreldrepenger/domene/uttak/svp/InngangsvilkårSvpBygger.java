package no.nav.foreldrepenger.domene.uttak.svp;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.svangerskapspenger.regler.fastsettperiode.grunnlag.Inngangsvilkår;

import java.util.Objects;

@ApplicationScoped
public class InngangsvilkårSvpBygger {

    InngangsvilkårSvpBygger() {
            //CDI
        }

        public Inngangsvilkår byggInngangsvilårSvp(VilkårResultat vilkårResultat) {
            return new Inngangsvilkår(opptjeningsvilkåretOppfylt(vilkårResultat),svangerskapsVilkåretOppfylt(vilkårResultat) );
        }

        public static boolean opptjeningsvilkåretOppfylt(VilkårResultat vilkårResultat) {
            return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.OPPTJENINGSVILKÅRET);
        }

        public static boolean svangerskapsVilkåretOppfylt(VilkårResultat vilkårResultat) {
        return vilkårAvTypeErOppfylt(vilkårResultat, VilkårType.SVANGERSKAPSPENGERVILKÅR);
        }

        private static boolean vilkårAvTypeErOppfylt(VilkårResultat vilkårResultat, VilkårType type) {
            var vilkår = vilkårResultat.getVilkårene()
                .stream()
                .filter(v -> Objects.equals(v.getVilkårType(), type))
                .findFirst();
            return vilkår.map(v -> !v.erIkkeOppfylt()).orElse(true);
        }
}
