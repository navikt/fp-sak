package no.nav.foreldrepenger.behandlingslager.geografisk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.Test;

class LandkoderTest {

    @Test
    void skal_sjekke_for_norge() {
        var avstand = Period.between(LocalDate.now(), LocalDate.now());
        System.out.println(avstand.getDays() + avstand.getMonths() * 30 + avstand.getYears() * 12 * 30);
        assertThat(Landkoder.erNorge("SWE")).isFalse();
        assertThat(Landkoder.erNorge("NOR")).isTrue();
    }

    @Test
    void sjekk_land_sortering() {
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA))).isEqualTo(Landkoder.USA);
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA, Landkoder.FRA))).isEqualTo(Landkoder.FRA);
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA, Landkoder.ITA, Landkoder.NOR))).isEqualTo(Landkoder.NOR);
    }
}
