package no.nav.foreldrepenger.domene.typer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BeløpTest {

    @Test
    void erNulltall_returnerer_false_for_null() {
        BigDecimal bd = null;
        var beløp = new Beløp(bd);
        var actual = beløp.erNulltall();
        assertThat(actual).isFalse();
    }

    @Test
    void erNulltall_returnerer_true_for_ZERO() {
        var bd = BigDecimal.ZERO.setScale(10);
        var beløp = new Beløp(bd);
        var actual = beløp.erNulltall();
        assertThat(actual).isTrue();
    }

    @Test
    void erNulltall_returnerer_false_for_liten_desimal() {
        var bd = BigDecimal.valueOf(0.00001);
        var beløp = new Beløp(bd);
        var actual = beløp.erNulltall();
        assertThat(actual).isFalse();
    }

    @Test
    void erNullEllerNulltall_detekterer_liten_desimal() {
        var bd = new BigDecimal(0.00001);
        var beløp = new Beløp(bd);
        var actual = beløp.erNullEllerNulltall();
        assertThat(actual).isFalse();
    }

    @Test
    void erNullEllerNulltall_detekterer_null() {
        BigDecimal bd = null;
        var beløp = new Beløp(bd);
        var actual = beløp.erNullEllerNulltall();
        assertThat(actual).isTrue();
    }

    @Test
    void erNullEllerNulltall_detekterer_nulltall() {
        var bd = BigDecimal.ZERO;
        var beløp = new Beløp(bd);
        var actual = beløp.erNullEllerNulltall();
        assertThat(actual).isTrue();
    }
}
