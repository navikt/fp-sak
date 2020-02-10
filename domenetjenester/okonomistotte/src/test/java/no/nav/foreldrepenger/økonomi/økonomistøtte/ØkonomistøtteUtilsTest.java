package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;

import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomistøtteUtils;

public class ØkonomistøtteUtilsTest {

    private String EXPECTED_DATETIME_STR = "2018-11-08-12.30.30.123";

    @Test
    public void testDateTimeTruncBasedAvrundingNed() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2018, 11, 8, 12, 30, 30, 123300000);
        // Act
        String datoOgKlokkeslett = ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(time);
        // Assert
        assertThat(datoOgKlokkeslett).isNotEmpty();
        assertThat(datoOgKlokkeslett).isEqualTo(EXPECTED_DATETIME_STR);
    }

    @Test
    public void testDateTimeTruncBasedAvrundingOpp() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2018, 11, 8, 12, 30, 30, 123700000);
        // Act
        String datoOgKlokkeslett = ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(time);
        // Assert
        assertThat(datoOgKlokkeslett).isNotEmpty();

        assertThat(datoOgKlokkeslett).isEqualTo(EXPECTED_DATETIME_STR);
    }

}
