package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public class VilkårHjemmelMapper {

    public static VilkårHjemmel mapVilkårTypeTilVilkårHjemmel(VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        switch (vilkårType) {
            case VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD -> {
                return VilkårHjemmel.F_14_17_1_ES;
            }
            case VilkårType.ADOPSJONSVILKARET_FORELDREPENGER -> {
                return VilkårHjemmel.F_14_5_1_FP;
                // TODO(siri) finn ut om vi skal defalt mappe til ledd 1 eller 3
                // return VilkårHjemmel.F_14_5_3_FP;
            }
            case VilkårType.OMSORGSVILKÅRET -> {
                return VilkårHjemmel.F_14_17_3_ES;
            }
            case VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD -> {
                if (fagsakYtelseType.equals(FagsakYtelseType.ENGANGSTØNAD)) {
                    return VilkårHjemmel.F_14_17_2_ES;
                } else {
                    return VilkårHjemmel.F_14_5_2_FP;
                }
            }
            case VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD -> {
                return VilkårHjemmel.F_14_17_4_ES;
            }
            default -> throw new IllegalArgumentException("Utviklerfeil: Klarer ikke mappe vilkårtype " + vilkårType + " til vilkårhjemmel.");
        }

    }
}
