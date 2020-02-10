package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Test;

import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.FinnAlleredeUtbetaltTom;

public class FinnAlleredeUtbetaltTomTest {

    @Test
    public void dagens_dato_4_februar() {
        // Act
        LocalDate utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.FEBRUARY, 4));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
    }

    @Test
    public void dagens_dato_15_februar() {
        // Act
        LocalDate utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.FEBRUARY, 15));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
    }

    @Test
    public void dagens_dato_16_februar() {
        // Act
        LocalDate utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.FEBRUARY, 16));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 28));
    }

    @Test
    public void dagens_dato_1_mars() {
        // Act
        LocalDate utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.MARCH, 1));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 28));
    }

    @Test
    public void dagens_dato_16_februar_2020() {
        // Act
        LocalDate utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2020, Month.FEBRUARY, 16));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2020, Month.FEBRUARY, 29));
    }
}
