package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HistorikkBelopTest {

    @Test
    void skal_lage_beløp_av_integer() {
        var fra = HistorikkBelop.ofNullable(12345);

        assertThat(fra.belop()).isEqualTo("12345");
        assertThat(fra).hasToString("12 345 kr");
    }

    @Test
    void skal_avrunde_beløp_av_bigdecimal() {
        assertThat(HistorikkBelop.ofNullable(BigDecimal.valueOf(35000.2))).hasToString("35 000 kr");
        assertThat(HistorikkBelop.ofNullable(BigDecimal.valueOf(35000.8))).hasToString("35 001 kr");
    }

    @Test
    void skal_håndtere_nullable_input() {
        assertThat(HistorikkBelop.ofNullable((BigDecimal) null)).isNull();
    }

}
