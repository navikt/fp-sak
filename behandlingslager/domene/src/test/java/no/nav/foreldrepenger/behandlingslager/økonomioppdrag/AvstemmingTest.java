package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

public class AvstemmingTest {

    @Test
    public void skal_bygge_instans_med_påkrevde_felter() {
        Avstemming avstemming = Avstemming.ny();
        validerObjekt(avstemming);
    }

    @Test
    void skal_bygge_instans_og_sette_riktig_nøkkel() {
        var localDateTime = LocalDateTime.now();
        Avstemming avstemming = Avstemming.fra(localDateTime);
        var expectedFormat = Avstemming.validateAndFormat(localDateTime);

        validerObjekt(avstemming);
        assertThat(avstemming.getNøkkel()).isEqualTo(expectedFormat);
    }

    @Test
    public void skal_feile_hvis_tidspunkt_er_null() {
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

    private void validerObjekt(Avstemming avstemming) {
        assertThat(avstemming.getNøkkel()).isNotNull();
        assertThat(avstemming.getTidspunkt()).isNotNull();
        assertThat(avstemming.getNøkkel()).isEqualTo(avstemming.getTidspunkt());
    }
}
