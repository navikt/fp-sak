package no.nav.foreldrepenger.økonomistøtte.ny.domene;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class YtelsePeriodeTest {

    @Test
    public void skal_summere_perioder_som_har_samme_typer() {
        LocalDate idag = LocalDate.now();
        Periode periode = Periode.of(idag, idag.plusDays(3));
        YtelsePeriode yp1 = new YtelsePeriode(periode, Satsen.dagsats(100));
        YtelsePeriode yp2 = new YtelsePeriode(periode, Satsen.dagsats(230));

        YtelsePeriode resultat = YtelsePeriode.summer(Arrays.asList(yp1, yp2));
        assertThat(resultat.getPeriode()).isEqualTo(periode);
        assertThat(resultat.getSats()).isEqualTo(Satsen.dagsats(330));
        assertThat(resultat.getUtbetalingsgrad()).isNull();
    }

    @Test
    public void skal_rapportere_sum_for_dagytelse() {
        LocalDate enTirsdag = LocalDate.of(2020, 11, 24);
        LocalDate nesteTirsdag = LocalDate.of(2020, 12, 1);
        Periode periode = Periode.of(enTirsdag, nesteTirsdag);
        YtelsePeriode yp = new YtelsePeriode(periode, Satsen.dagsats(100));
        assertThat(yp.summerYtelse()).isEqualTo(600); //6 virkedager (2 tirsdager) ganger 100
    }

    @Test
    public void skal_rapportere_sum_for_engangsutbetaling() {
        LocalDate førsteMai = LocalDate.of(2020, 5, 1);
        LocalDate sisteMai = LocalDate.of(2020, 5, 31);
        Periode periode = Periode.of(førsteMai, sisteMai);
        YtelsePeriode yp = new YtelsePeriode(periode, Satsen.engang(252));
        assertThat(yp.summerYtelse()).isEqualTo(252);
    }
}
