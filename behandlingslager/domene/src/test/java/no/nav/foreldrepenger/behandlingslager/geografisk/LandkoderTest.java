package no.nav.foreldrepenger.behandlingslager.geografisk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class LandkoderTest {

    @Test
    public void skal_sjekke_for_norge() {
        assertThat(Landkoder.erNorge("SWE")).isFalse();
        assertThat(Landkoder.erNorge("NOR")).isTrue();
    }

    @Test
    public void sjekk_land_sortering() {
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA))).isEqualTo(Landkoder.USA);
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA, Landkoder.GBR))).isEqualTo(Landkoder.GBR);
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA, Landkoder.GBR, Landkoder.NOR))).isEqualTo(Landkoder.NOR);
    }
}
