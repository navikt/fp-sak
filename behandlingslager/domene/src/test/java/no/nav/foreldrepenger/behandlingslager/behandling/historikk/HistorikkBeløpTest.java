package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HistorikkBeløpTest {

    @Test
    void skal_lage_beløp_av_integer() {
        var fra = HistorikkBeløp.ofNullable(12345);

        assertThat(fra.beløp()).isEqualTo("12345");
        assertThat(fra).hasToString("12 345 kr");
    }

    @Test
    void skal_avrunde_beløp_av_bigdecimal() {
        assertThat(HistorikkBeløp.ofNullable(BigDecimal.valueOf(35000.2))).hasToString("35 000 kr");
        assertThat(HistorikkBeløp.ofNullable(BigDecimal.valueOf(35000.8))).hasToString("35 001 kr");
    }

    @Test
    void skal_håndtere_nullable_input() {
        assertThat(HistorikkBeløp.ofNullable((BigDecimal) null)).isNull();
    }

    @Test
    void skal_feile_for_null_beløp_ved_forventet_beløp() {
        assertThatIllegalArgumentException().isThrownBy(() -> HistorikkBeløp.of((BigDecimal) null)).withMessage("Beløp cannot be null");
    }

}
