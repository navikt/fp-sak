package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * a == helTilrettelegging
 * b == delvisTilrettelegging
 * c == slutteArbeid
 *
 * Reflekterer hva søker oppgir i SVP-søknaden.
 */
public class BeregnTilrettleggingsdatoTest {

    @Test
    public void skal_gi_C_hvis_C_er_oppgitt_og_C_er_senere_enn_jordmorsdato() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate c = LocalDate.of(2019, 5, 20);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_B_er_oppgitt_og_B_er_senere_enn_jordmorsdato() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate b = LocalDate.of(2019, 5, 20);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.empty());
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_B_er_oppgitt_og_B_er_lik_jordmorsdato() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate b = LocalDate.of(2019, 5, 10);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.empty());
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_C_er_oppgitt_og_C_er_lik_jordmorsdato() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate c = LocalDate.of(2019, 5, 10);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    public void skal_gi_C_hvis_A_og_C_er_oppgitt() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate a = LocalDate.of(2019, 5, 10);
        LocalDate c = LocalDate.of(2019, 5, 20);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    public void skal_gi_C_hvis_B_og_C_er_oppgitt_og_c_er_tidligere_enn_B() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate b = LocalDate.of(2019, 5, 15);
        LocalDate c = LocalDate.of(2019, 5, 10);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    public void skal_gi_C_hvis_A_og_B_og_C_er_oppgitt_og_C_er_tidligere_enn_B() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate a = LocalDate.of(2019, 5, 10);
        LocalDate c = LocalDate.of(2019, 5, 15);
        LocalDate b = LocalDate.of(2019, 5, 20);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(c);
    }

    @Test
    public void skal_gi_B_hvis_A_og_B_og_C_er_oppgitt_og_B_er_tidligere_enn_C() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate a = LocalDate.of(2019, 5, 10);
        LocalDate b = LocalDate.of(2019, 5, 15);
        LocalDate c = LocalDate.of(2019, 5, 20);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(b);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_A_og_B_og_C_er_oppgitt_og_alle_er_før_jordmorsdato() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate a = LocalDate.of(2019, 5, 2);
        LocalDate b = LocalDate.of(2019, 5, 5);
        LocalDate c = LocalDate.of(2019, 5, 3);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    public void skal_gi_B_hvis_A_og_B_og_C_er_oppgitt_og_B_er_før_C() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate a = LocalDate.of(2019, 5, 10);
        LocalDate b = LocalDate.of(2019, 5, 15);
        LocalDate c = LocalDate.of(2019, 5, 20);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(b);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_B_og_C_er_oppgitt_og_B_er_tidligere_enn_C() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 10);
        LocalDate b = LocalDate.of(2019, 5, 12);
        LocalDate c = LocalDate.of(2019, 5, 15);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.empty(), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_A_og_C_er_oppgitt_og_A_er_før_C_og_ulik_jordmorsdato() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 1);
        LocalDate a = LocalDate.of(2019, 6, 1);
        LocalDate c = LocalDate.of(2019, 7, 1);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.empty(), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_A_og_B_er_oppgitt_og_A_er_før_B_og_ulik_jordmorsdato() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 1);
        LocalDate a = LocalDate.of(2019, 6, 1);
        LocalDate b = LocalDate.of(2019, 7, 1);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.empty());
        assertThat(beregn).isEqualTo(jordmorsdato);
    }

    @Test
    public void skal_gi_jordmorsdato_hvis_A_og_B_og_C_er_oppgitt_og_A_er_tidligere_enn_B_og_C() {
        LocalDate jordmorsdato = LocalDate.of(2019, 5, 1);
        LocalDate a = LocalDate.of(2019, 5, 5);
        LocalDate c = LocalDate.of(2019, 5, 10);
        LocalDate b = LocalDate.of(2019, 5, 15);

        LocalDate beregn = BeregnTilrettleggingsdato.beregn(jordmorsdato, Optional.of(a), Optional.of(b), Optional.of(c));
        assertThat(beregn).isEqualTo(jordmorsdato);
    }
}
