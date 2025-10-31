package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AvslagsårsakVilkårTypeMappingTest {

    @Test
    void test_finn_vilkårtype_fra_avslagårsak() {
        assertThat(Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O.getVilkårTyper()).contains(VilkårType.OMSORGSOVERTAKELSEVILKÅR);
    }

    @Test
    void skal_hente_alle_avslagsårsaker_gruppert_på_vilkårstype() {
        var map = VilkårType.finnAvslagårsakerGruppertPåVilkårType();
        assertThat(map.get(VilkårType.SØKERSOPPLYSNINGSPLIKT)).containsOnly(Avslagsårsak.MANGLENDE_DOKUMENTASJON);
        assertThat(map.get(VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR))
                .containsOnly(Avslagsårsak.INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR,
                    Avslagsårsak.MOR_FYLLER_IKKE_VILKÅRET_FOR_SYKDOM,
                    Avslagsårsak.BRUKER_ER_IKKE_REGISTRERT_SOM_FAR_MEDMOR_TIL_BARNET,
                    Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR);
    }
}
