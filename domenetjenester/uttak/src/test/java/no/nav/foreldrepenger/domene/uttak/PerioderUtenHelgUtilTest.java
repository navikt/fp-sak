package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.domene.uttak.PerioderUtenHelgUtil.datoerLikeNårHelgIgnoreres;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PerioderUtenHelgUtilTest {


    private static final LocalDate SØNDAG_FØR_UKE_1 = LocalDate.of(2017, 1, 1);

    private final LocalDate fredagUke1 = dag(DayOfWeek.FRIDAY, 1);
    private final LocalDate lørdagUke1 = dag(DayOfWeek.SATURDAY, 1);
    private final LocalDate søndagUke1 = dag(DayOfWeek.SUNDAY, 1);
    private final LocalDate mandagUke2 = dag(DayOfWeek.MONDAY, 2);
    private final LocalDate tirsdagUke2 = dag(DayOfWeek.TUESDAY, 2);

    @Test
    void skal_si_at_datoer_er_like() {
        assertThat(datoerLikeNårHelgIgnoreres(lørdagUke1, søndagUke1)).isTrue();
        assertThat(datoerLikeNårHelgIgnoreres(lørdagUke1, mandagUke2)).isTrue();
    }

    @Test
    void skal_si_at_datoer_er_ulike() {
        assertThat(datoerLikeNårHelgIgnoreres(fredagUke1, mandagUke2)).isFalse();
        assertThat(datoerLikeNårHelgIgnoreres(tirsdagUke2, lørdagUke1)).isFalse();
        assertThat(datoerLikeNårHelgIgnoreres(fredagUke1, lørdagUke1)).isFalse();
    }

    private LocalDate dag(DayOfWeek dag, int ukenr) {
        return SØNDAG_FØR_UKE_1.plusWeeks(ukenr - 1).plusDays(dag.getValue());
    }
}
