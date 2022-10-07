package no.nav.foreldrepenger.behandlingslager.geografisk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class LandkoderTest {

    @Test
    public void skal_sjekke_for_norge() {
        LocalDate ref = LocalDate.now();
        List<LocalDateSegment<Integer>> tliste = new ArrayList<>();
        var tomListe = new LocalDateTimeline<>(tliste);
        var innholdListe = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(ref.minusDays(10), ref.minusDays(5), 1),
            new LocalDateSegment<>(ref.plusDays(5), ref.plusDays(10), 1)));
        var innholdListe2 = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(ref.minusDays(15), ref.minusDays(7), 1),
            new LocalDateSegment<>(ref.plusDays(8), ref.plusDays(15), 2)));
        var diff = innholdListe2.combine(innholdListe, (i, lhs, rhs) -> new LocalDateSegment<>(i, !Objects.equals(lhs, rhs)), LocalDateTimeline.JoinStyle.CROSS_JOIN);
        System.out.println(diff.filterValue(v -> v));
        assertThat(Landkoder.erNorge("SWE")).isFalse();
        assertThat(Landkoder.erNorge("NOR")).isTrue();
    }

    @Test
    public void sjekk_land_sortering() {
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA))).isEqualTo(Landkoder.USA);
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA, Landkoder.FRA))).isEqualTo(Landkoder.FRA);
        assertThat(MapRegionLandkoder.finnRangertLandkode(List.of(Landkoder.USA, Landkoder.ITA, Landkoder.NOR))).isEqualTo(Landkoder.NOR);
    }
}
