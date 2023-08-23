package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AvstemmingTest {

    @Test
    void skal_bygge_instans_med_påkrevde_felter() {
        var avstemming = Avstemming.ny();
        validerObjekt(avstemming);
    }

    @Test
    void skal_bygge_instans_og_sette_riktig_nøkkel() {
        var localDateTime = LocalDateTime.now();
        var avstemming = Avstemming.fra(localDateTime);
        var expectedFormat = Avstemming.validateAndFormat(localDateTime);

        validerObjekt(avstemming);
        assertThat(avstemming.getNøkkel()).isEqualTo(expectedFormat);
    }

    @Test
    void skal_feile_hvis_tidspunkt_er_null() {
        Exception thrown = assertThrows(
            NullPointerException.class,
            () -> Avstemming.fra(null)
        );

        assertTrue(thrown.getMessage().contains("avstemmingTidspunkt"));
    }

    @Test
    void skal_være_lik_hvis_samme_dato_valgt() {
        var testDato = LocalDateTime.now();
        var avstemming1 = Avstemming.fra(testDato);
        var avstemming2 =  Avstemming.fra(testDato);

        assertEquals(avstemming1, avstemming2);
        assertEquals(avstemming2.hashCode(), avstemming1.hashCode());
    }

    @Test
    void skal_være_ulik_hvis_forskjellig_dato_valgt() {
        var testDato = LocalDateTime.now();
        var avstemming1 = Avstemming.fra(testDato);
        var avstemming2 =  Avstemming.fra(testDato.plusDays(1));

        assertNotEquals(avstemming1, avstemming2);
        assertNotEquals(avstemming2.hashCode(), avstemming1.hashCode());
    }

    @Test
    void test_compareTo() {
        var testDato = LocalDateTime.now();
        var avstemming1 = Avstemming.fra(testDato);
        var avstemming2 =  Avstemming.fra(testDato.plusDays(1));

        assertThat(avstemming1.compareTo(avstemming2)).isEqualTo(-1);
        assertThat(avstemming2.compareTo(avstemming1)).isEqualTo(1);
        assertThat(avstemming1.compareTo(avstemming1)).isZero();
    }

    private void validerObjekt(Avstemming avstemming) {
        assertThat(avstemming.getNøkkel()).isNotNull();
        assertThat(avstemming.getTidspunkt()).isNotNull();
        assertThat(avstemming.getNøkkel()).isEqualTo(avstemming.getTidspunkt());
    }
}
