package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;

class FinnAlleredeUtbetaltTomTest {

    @Test
    void dagens_dato_4_februar() {
        // Act
        var utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.FEBRUARY, 4));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
    }

    @Test
    void dagens_dato_15_februar() {
        // Act
        var utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.FEBRUARY, 18));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.JANUARY, 31));
    }

    @Test
    void dagens_dato_16_februar() {
        // Act
        var utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.FEBRUARY, 19));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 28));
    }

    @Test
    void dagens_dato_1_mars() {
        // Act
        var utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2019, Month.MARCH, 1));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2019, Month.FEBRUARY, 28));
    }

    @Test
    void dagens_dato_16_februar_2020() {
        // Act
        var utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2020, Month.FEBRUARY, 19));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2020, Month.FEBRUARY, 29));
    }

    @Test
    void dagens_dato_8_desember_2020() {
        // Act
        var utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2020, Month.DECEMBER, 8));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2020, Month.DECEMBER, 31));
    }

    @Test
    void dagens_dato_7_desember_2020() {
        // Act
        var utbetaltTom = FinnAlleredeUtbetaltTom.finn(LocalDate.of(2020, Month.DECEMBER, 7));

        // Assert
        assertThat(utbetaltTom).isEqualTo(LocalDate.of(2020, Month.NOVEMBER, 30));
    }

}
