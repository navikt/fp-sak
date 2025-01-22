package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SatsTest {


    @Test
    void skal_bygge_instans_med_påkrevde_felter_sats_1() {
        var gradVerdi = 1;
        var sats = Sats.på(gradVerdi);
        validerObjekt(sats, gradVerdi);
    }

    @Test
    void skal_bygge_instans_med_påkrevde_felter_sats_100() {
        validerObjekt(Sats.på(100L), 100);
    }

    @Test
    void skal_feile_hvis_sats_er_null() {
        Exception thrown = assertThrows(
            NullPointerException.class,
            () -> Sats.på(null)
        );

        assertThat(thrown.getMessage().contains("sats")).isTrue();
    }

    @Test
    void skal_feile_hvis_sats_er_mindre_en_0() {
        var verdi = -100;
        Exception thrown = assertThrows(IllegalArgumentException.class, () -> Sats.på(verdi));
        assertThat(thrown.getMessage().contains("Sats er utenfor lovlig intervall")).isTrue();
    }

    @Test
    void skal_feile_hvis_sats_er_0() {
        var sats = Sats.på(0);
        validerObjekt(sats, 0);
    }

    @Test
    void skal_være_lik_hvis_samme_sats_valgt() {
        var testSats = 2342 ;
        var sats = Sats.på(testSats);
        var sats2 = Sats.på(testSats);

        assertThat(sats).isEqualTo(sats2);
        assertThat(sats2.hashCode()).isEqualTo(sats.hashCode());
    }

    @Test
    void skal_være_ulik_hvis_forskjellig_prosent_valgt() {
        var testSats = 40;
        var grad1 = Sats.på(testSats);
        var grad2 = Sats.på(testSats + 10);

        assertThat(grad1).isNotEqualTo(grad2);
        assertThat(grad2.hashCode()).isNotEqualTo(grad1.hashCode());
    }

    @Test
    void skal_rundes_av_riktig() {
        validerObjekt(Sats.på(BigDecimal.valueOf(Math.PI)), 3);
    }

    private void validerObjekt(Sats sats, Integer expectedSats) {
        assertThat(sats.getVerdi()).isEqualTo(expectedSats);
    }

}
