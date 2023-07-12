package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Optional;

import no.nav.vedtak.exception.TekniskException;

public class AvslagsårsakMapper {

    private AvslagsårsakMapper() {
    }

    public static Avslagsårsak finnAvslagsårsak(Vilkår vilkår) {
        return vilkår.getGjeldendeAvslagsårsak().orElseGet(() -> fraVilkår(vilkår));
    }

    private static Avslagsårsak fraVilkår(Vilkår vilkår) {
        if (vilkår.getVilkårUtfallMerknad() == null) {
            var avslagsårsaker = vilkår.getVilkårType().getAvslagsårsaker();
            if (avslagsårsaker.size() != 1) {
                throw new TekniskException("FP-411111",
                    "Kan ikke utlede avslagsårsak, utfallmerknad mangler i vilkår " + vilkår.getVilkårType().getKode());
            }
            return avslagsårsaker.iterator().next();
        } else {
            return Optional.ofNullable(fraVilkårUtfallMerknad(vilkår.getVilkårUtfallMerknad()))
                .orElseThrow(() -> new TekniskException("FP-411110", "Kan ikke utlede avslagsårsak fra utfallmerknad " + vilkår.getVilkårUtfallMerknad().getKode()));
        }
    }

    public static Avslagsårsak fraVilkårUtfallMerknad(VilkårUtfallMerknad vilkårUtfallMerknad) {
        return Avslagsårsak.fraKode(vilkårUtfallMerknad.getKode());
    }
}
