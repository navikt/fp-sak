package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class AvslagsårsakTjeneste {

    @Inject
    public AvslagsårsakTjeneste() {
    }

    public Avslagsårsak finnAvslagsårsak(Vilkår vilkår) {
        return vilkår.getGjeldendeAvslagsårsak().orElseGet(() -> {
            if (vilkår.getVilkårUtfallMerknad() == null) {
                var avslagsårsaker = vilkår.getVilkårType().getAvslagsårsaker();
                if (avslagsårsaker.size() != 1) {
                    throw new TekniskException("FP-411111",
                        "Kan ikke utlede avslagsårsak, utfallmerknad mangler i vilkår " + vilkår.getVilkårType()
                            .getKode());
                }
                return avslagsårsaker.iterator().next();
            }
            var aå = Avslagsårsak.fraKode(vilkår.getVilkårUtfallMerknad().getKode());
            if (aå == null) {
                throw new TekniskException("FP-411110",
                    "Kan ikke utlede avslagsårsak fra utfallmerknad " + vilkår.getVilkårUtfallMerknad().getKode());
            }
            return aå;
        });
    }
}
