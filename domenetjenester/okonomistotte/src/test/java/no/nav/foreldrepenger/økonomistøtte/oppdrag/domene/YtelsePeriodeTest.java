package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class YtelsePeriodeTest {

    @Test
    void skal_summere_perioder_som_har_samme_typer() {
        var idag = LocalDate.now();
        var periode = Periode.of(idag, idag.plusDays(3));
        var yp1 = new YtelsePeriode(periode, Satsen.dagsats(100));
        var yp2 = new YtelsePeriode(periode, Satsen.dagsats(230));

        var resultat = YtelsePeriode.summer(Arrays.asList(yp1, yp2));
        assertThat(resultat.getPeriode()).isEqualTo(periode);
        assertThat(resultat.getSats()).isEqualTo(Satsen.dagsats(330));
        assertThat(resultat.getUtbetalingsgrad()).isNull();
    }

    @Test
    void skal_rapportere_sum_for_dagytelse() {
        var enTirsdag = LocalDate.of(2020, 11, 24);
        var nesteTirsdag = LocalDate.of(2020, 12, 1);
        var periode = Periode.of(enTirsdag, nesteTirsdag);
        var yp = new YtelsePeriode(periode, Satsen.dagsats(100));
        assertThat(yp.summerYtelse()).isEqualTo(600); //6 virkedager (2 tirsdager) ganger 100
    }

    @Test
    void skal_rapportere_sum_for_engangsutbetaling() {
        var førsteMai = LocalDate.of(2020, 5, 1);
        var sisteMai = LocalDate.of(2020, 5, 31);
        var periode = Periode.of(førsteMai, sisteMai);
        var yp = new YtelsePeriode(periode, Satsen.engang(252));
        assertThat(yp.summerYtelse()).isEqualTo(252);
    }
}
