package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * a == helTilrettelegging
 * b == delvisTilrettelegging
 * c == slutteArbeid
 *
 * Reflekterer hva søker oppgir i SVP-søknaden.
 */
class BeregnTilrettleggingsdatoTest {

    @Test
    void skal_gi_C_hvis_C_er_oppgitt_og_C_er_senere_enn_jordmorsdato() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var c = LocalDate.of(2019, 5, 20);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_B_er_oppgitt_og_B_er_senere_enn_jordmorsdato() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var b = LocalDate.of(2019, 5, 20);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.empty());
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_B_er_oppgitt_og_B_er_lik_jordmorsdato() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var b = LocalDate.of(2019, 5, 10);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.empty());
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_C_er_oppgitt_og_C_er_lik_jordmorsdato() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var c = LocalDate.of(2019, 5, 10);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    void skal_gi_C_hvis_A_og_C_er_oppgitt() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var a = LocalDate.of(2019, 5, 10);
        var c = LocalDate.of(2019, 5, 20);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    void skal_gi_C_hvis_B_og_C_er_oppgitt_og_c_er_tidligere_enn_B() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var b = LocalDate.of(2019, 5, 15);
        var c = LocalDate.of(2019, 5, 10);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    void skal_gi_C_hvis_A_og_B_og_C_er_oppgitt_og_C_er_tidligere_enn_B() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var a = LocalDate.of(2019, 5, 10);
        var c = LocalDate.of(2019, 5, 15);
        var b = LocalDate.of(2019, 5, 20);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    void skal_gi_B_hvis_A_og_B_og_C_er_oppgitt_og_B_er_tidligere_enn_C() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var a = LocalDate.of(2019, 5, 10);
        var b = LocalDate.of(2019, 5, 15);
        var c = LocalDate.of(2019, 5, 20);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(b);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_A_og_B_og_C_er_oppgitt_og_alle_er_før_jordmorsdato() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var a = LocalDate.of(2019, 5, 2);
        var b = LocalDate.of(2019, 5, 5);
        var c = LocalDate.of(2019, 5, 3);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    void skal_gi_B_hvis_A_og_B_og_C_er_oppgitt_og_B_er_før_C() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var a = LocalDate.of(2019, 5, 10);
        var b = LocalDate.of(2019, 5, 15);
        var c = LocalDate.of(2019, 5, 20);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(b);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_B_og_C_er_oppgitt_og_B_er_tidligere_enn_C() {
        var jordmorsdato = LocalDate.of(2019, 5, 10);
        var b = LocalDate.of(2019, 5, 12);
        var c = LocalDate.of(2019, 5, 15);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_A_og_C_er_oppgitt_og_A_er_før_C_og_ulik_jordmorsdato() {
        var jordmorsdato = LocalDate.of(2019, 5, 1);
        var a = LocalDate.of(2019, 6, 1);
        var c = LocalDate.of(2019, 7, 1);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_A_og_B_er_oppgitt_og_A_er_før_B_og_ulik_jordmorsdato() {
        var jordmorsdato = LocalDate.of(2019, 5, 1);
        var a = LocalDate.of(2019, 6, 1);
        var b = LocalDate.of(2019, 7, 1);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.empty());
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    void skal_gi_jordmorsdato_hvis_A_og_B_og_C_er_oppgitt_og_A_er_tidligere_enn_B_og_C() {
        var jordmorsdato = LocalDate.of(2019, 5, 1);
        var a = LocalDate.of(2019, 5, 5);
        var c = LocalDate.of(2019, 5, 10);
        var b = LocalDate.of(2019, 5, 15);

        var beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }
}
