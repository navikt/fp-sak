package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AvslagsårsakVilkårTypeMappingTest {

    @Test
    void test_finn_vilkårtype_fra_avslagårsak() {
        assertThat(Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O.getVilkårTyper()).contains(VilkårType.OMSORGSVILKÅRET);
    }

    @Test
    void skal_hente_alle_avslagsårsaker_gruppert_på_vilkårstype() {
        var map = VilkårType.finnAvslagårsakerGruppertPåVilkårType();
        assertThat(map.get(VilkårType.SØKERSOPPLYSNINGSPLIKT)).containsOnly(Avslagsårsak.MANGLENDE_DOKUMENTASJON);
        assertThat(map.get(VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD))
                .containsOnly(Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F, Avslagsårsak.OMSORGSOVERTAKELSE_ETTER_56_UKER,
                        Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA,
                        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
                        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR);
    }
}
