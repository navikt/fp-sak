package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UtbetalingsgradTest {

    @Test
    void skal_bygge_instans_med_påkrevde_felter_utbetalingsgrad_0() {
        var gradVerdi = 0;
        var utbetalingsgrad = Utbetalingsgrad.prosent(gradVerdi);
        validerObjekt(utbetalingsgrad, gradVerdi);
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter_utbetalingsgrad_100() {
        validerObjekt(Utbetalingsgrad._100, 100);
    }

    @Test
    void skal_feile_hvis_utbetalingsgrad_er_null() {
        Exception thrown = assertThrows(
            NullPointerException.class,
            () -> Utbetalingsgrad.prosent(null)
        );

        assertTrue(thrown.getMessage().contains("utbetalingsgrad"));
    }

    @Test
    void skal_feile_hvis_utbetalingsgrad_er_mindre_en_0() {
        final var verdi = -100;
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> {
                Utbetalingsgrad.prosent(verdi);
            }
        );
        assertTrue(thrown.getMessage().contains("Utbetalingsgrad er utenfor lovlig intervall [0,100]: " + verdi));
    }

    @Test
    void skal_feile_hvis_utbetalingsgrad_er_storre_en_100() {
        final var verdi = 110;
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> {
                Utbetalingsgrad.prosent(verdi);
            }
        );
        assertTrue(thrown.getMessage().contains("Utbetalingsgrad er utenfor lovlig intervall [0,100]: " + verdi));
    }

    @Test
    void skal_være_lik_hvis_samme_prosent_valgt() {
        var testProsent = 40;
        var grad1 = Utbetalingsgrad.prosent(testProsent);
        var grad2 = Utbetalingsgrad.prosent(testProsent);

        assertEquals(grad1, grad2);
        assertEquals(grad2.hashCode(), grad1.hashCode());
    }

    @Test
    void skal_være_ulik_hvis_forskjellig_prosent_valgt() {
        var testProsent = 40;
        var grad1 = Utbetalingsgrad.prosent(testProsent);
        var grad2 = Utbetalingsgrad.prosent(testProsent + 10);

        assertNotEquals(grad1, grad2);
        assertNotEquals(grad2.hashCode(), grad1.hashCode());
    }

    private void validerObjekt(Utbetalingsgrad utbetalingsgrad, Integer expectedGrad) {
        assertThat(utbetalingsgrad.getVerdi()).isEqualTo(expectedGrad);
    }

}
