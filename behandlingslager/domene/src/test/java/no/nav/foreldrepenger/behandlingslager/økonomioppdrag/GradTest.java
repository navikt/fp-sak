package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GradTest {

    private static final String UFOR = "UFOR";

    @Test
    public void skal_bygge_instans_med_påkrevde_felter_grad_0() {
        var gradVerdi = 0;
        Grad grad = Grad.prosent(gradVerdi);
        validerObjekt(grad, gradVerdi);
    }

    @Test
    public void skal_bygge_instans_med_påkrevde_felter_grad_100() {
        validerObjekt(Grad._100, 100);
    }

    @Test
    public void skal_alltid_returnere_UFOR_som_type_grad() {
        var gradVerdi = 50;
        Grad grad = Grad.prosent(gradVerdi);

        assertThat(grad.getType()).isEqualTo(UFOR);
        validerObjekt(grad, gradVerdi);
    }

    @Test
    public void skal_feile_hvis_grad_er_null() {
        Exception thrown = assertThrows(
            NullPointerException.class,
            () -> Grad.prosent(null)
        );

        assertTrue(thrown.getMessage().contains("grad"));
    }

    @Test
    public void skal_feile_hvis_grad_er_mindre_en_0() {
        final var verdi = -100;
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> {
                Grad.prosent(verdi);
            }
        );
        assertTrue(thrown.getMessage().contains("Grad er utenfor lovlig intervall [0,100]: " + verdi));
    }

    @Test
    public void skal_feile_hvis_grad_er_storre_en_100() {
        final var verdi = 110;
        Exception thrown = assertThrows(
            IllegalArgumentException.class,
            () -> {
                Grad.prosent(verdi);
            }
        );
        assertTrue(thrown.getMessage().contains("Grad er utenfor lovlig intervall [0,100]: " + verdi));
    }

    @Test
    void skal_være_lik_hvis_samme_prosent_valgt() {
        var testProsent = 40;
        var grad1 = Grad.prosent(testProsent);
        var grad2 = Grad.prosent(testProsent);

        assertEquals(grad1, grad2);
        assertEquals(grad2.hashCode(), grad1.hashCode());
    }

    @Test
    void skal_være_ulik_hvis_forskjellig_prosent_valgt() {
        var testProsent = 40;
        var grad1 = Grad.prosent(testProsent);
        var grad2 = Grad.prosent(testProsent + 10);

        assertNotEquals(grad1, grad2);
        assertNotEquals(grad2.hashCode(), grad1.hashCode());
    }

    private void validerObjekt(Grad grad, Integer expectedGrad) {
        assertThat(grad.getVerdi()).isEqualTo(expectedGrad);
        assertThat(grad.getType()).isEqualTo(UFOR);
    }

}
