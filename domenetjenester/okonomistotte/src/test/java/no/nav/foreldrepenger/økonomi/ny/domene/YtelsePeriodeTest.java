package no.nav.foreldrepenger.økonomi.ny.domene;

import java.time.LocalDate;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class YtelsePeriodeTest {

    @Test
    public void skal_summere_perioder_som_har_samme_typer() {
        LocalDate idag = LocalDate.now();
        Periode periode = Periode.of(idag, idag.plusDays(3));
        YtelsePeriode yp1 = new YtelsePeriode(periode, Sats.dagsats(100));
        YtelsePeriode yp2 = new YtelsePeriode(periode, Sats.dagsats(230));

        YtelsePeriode resultat = YtelsePeriode.summer(Arrays.asList(yp1, yp2));
        Assertions.assertThat(resultat.getPeriode()).isEqualTo(periode);
        Assertions.assertThat(resultat.getSats()).isEqualTo(Sats.dagsats(330));
        Assertions.assertThat(resultat.getUtbetalingsgrad()).isNull();
    }
}
