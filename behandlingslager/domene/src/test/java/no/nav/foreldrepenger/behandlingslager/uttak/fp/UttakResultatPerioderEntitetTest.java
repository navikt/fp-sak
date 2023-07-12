package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;

class UttakResultatPerioderEntitetTest {

    @Test
    void periode_skal_sorteres_når_de_hentes_ut() {
        var start = LocalDate.now();
        var perioder = new UttakResultatPerioderEntitet();
        // Legger til periodene i feil rekkefølge
        perioder.leggTilPeriode(lagPeriode(start.plusWeeks(2), start.plusWeeks(3).minusDays(1)));
        perioder.leggTilPeriode(lagPeriode(start.plusWeeks(1), start.plusWeeks(2).minusDays(1)));
        perioder.leggTilPeriode(lagPeriode(start, start.plusWeeks(1).minusDays(1)));

        var sortertePerioder = perioder.getPerioder();
        assertThat(sortertePerioder).hasSize(3);
        assertThat(sortertePerioder.get(0).getFom()).isEqualTo(start);
        assertThat(sortertePerioder.get(0).getTom()).isEqualTo(start.plusWeeks(1).minusDays(1));
        assertThat(sortertePerioder.get(1).getFom()).isEqualTo(start.plusWeeks(1));
        assertThat(sortertePerioder.get(1).getTom()).isEqualTo(start.plusWeeks(2).minusDays(1));
        assertThat(sortertePerioder.get(2).getFom()).isEqualTo(start.plusWeeks(2));
        assertThat(sortertePerioder.get(2).getTom()).isEqualTo(start.plusWeeks(3).minusDays(1));
    }

    @Test
    void skal_feile_dersom_det_blir_lagt_til_overlappende_perioder() {
        var start = LocalDate.now();
        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(lagPeriode(start, start.plusWeeks(6)));
        var nyPeriodeSomOverlapper = lagPeriode(start.plusWeeks(6), start.plusWeeks(16).minusDays(1));
        assertThrows(IllegalArgumentException.class, () -> perioder.leggTilPeriode(nyPeriodeSomOverlapper));
    }

    private UttakResultatPeriodeEntitet lagPeriode(LocalDate fom, LocalDate tom) {
        return new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT).build();
    }

}
