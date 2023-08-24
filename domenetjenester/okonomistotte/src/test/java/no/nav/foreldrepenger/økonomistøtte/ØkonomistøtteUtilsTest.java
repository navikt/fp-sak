package no.nav.foreldrepenger.økonomistøtte;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ØkonomistøtteUtilsTest {

    private String EXPECTED_DATETIME_STR = "2018-11-08-12.30.30.123";

    @Test
    void testDateTimeTruncBasedAvrundingNed() {
        // Arrange
        var time = LocalDateTime.of(2018, 11, 8, 12, 30, 30, 123300000);
        // Act
        var datoOgKlokkeslett = ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(time);
        // Assert
        assertThat(datoOgKlokkeslett).isNotEmpty();
        assertThat(datoOgKlokkeslett).isEqualTo(EXPECTED_DATETIME_STR);
    }

    @Test
    void testDateTimeTruncBasedAvrundingOpp() {
        // Arrange
        var time = LocalDateTime.of(2018, 11, 8, 12, 30, 30, 123700000);
        // Act
        var datoOgKlokkeslett = ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(time);
        // Assert
        assertThat(datoOgKlokkeslett).isNotEmpty();

        assertThat(datoOgKlokkeslett).isEqualTo(EXPECTED_DATETIME_STR);
    }

}
