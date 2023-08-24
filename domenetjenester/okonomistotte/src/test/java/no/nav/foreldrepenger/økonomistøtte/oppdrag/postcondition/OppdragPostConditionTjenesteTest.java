package no.nav.foreldrepenger.økonomistøtte.oppdrag.postcondition;


import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OppdragPostConditionTjenesteTest {

    LocalDate forrigeSøndag = LocalDate.of(2020, 11, 22);
    LocalDate mandag = forrigeSøndag.plusDays(1);
    LocalDate fredag = mandag.plusDays(4);
    LocalDate lørdag = mandag.plusDays(5);
    LocalDate søndag = mandag.plusDays(6);
    LocalDate nesteMandag = mandag.plusDays(7);

    @Test
    void skal_ikke_finne_differanse_mellom_identiske_ytelser() {
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(Ytelse.EMPTY, Ytelse.EMPTY, Betalingsmottaker.BRUKER)).isEmpty();
    }

    @Test
    void skal_ignorere_helger_for_satstype_dagsats() {
        var ytelseKontinuerlig = Ytelse.builder()
            .leggTilPeriode(new YtelsePeriode(Periode.of(mandag, fredag), Satsen.dagsats(100)))
            .leggTilPeriode(new YtelsePeriode(Periode.of(nesteMandag, nesteMandag), Satsen.dagsats(100)))
            .build();
        var ytelseSplittet = Ytelse.builder().leggTilPeriode(
            new YtelsePeriode(Periode.of(mandag, nesteMandag), Satsen.dagsats(100)))
            .build();
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseKontinuerlig, ytelseSplittet, Betalingsmottaker.BRUKER)).isEmpty();
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseSplittet, ytelseKontinuerlig, Betalingsmottaker.BRUKER)).isEmpty();
    }

    @Test
    void skal_finne_differanse() {
        var ytelse1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, mandag), Satsen.dagsats(100))).build();

        assertThat(OppdragPostConditionTjeneste.finnDifferanse(Ytelse.EMPTY, ytelse1, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, 100));
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelse1, Ytelse.EMPTY, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, -100));

        var ytelse1SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Satsen.dagsats(100))).build();
        var ytelse2SøndagSøndag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(forrigeSøndag, søndag), Satsen.dagsats(200))).build();
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelse1SøndagSøndag, ytelse2SøndagSøndag, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, 500));
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelse2SøndagSøndag, ytelse1SøndagSøndag, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(mandag, null, -500));

        var ytelseUke1 = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, lørdag), Satsen.dagsats(100))).build();
        var ytelseUke1OgNesteMandag = Ytelse.builder().leggTilPeriode(new YtelsePeriode(Periode.of(mandag, nesteMandag), Satsen.dagsats(100))).build();

        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseUke1, ytelseUke1OgNesteMandag, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(nesteMandag, null, 100));
        assertThat(OppdragPostConditionTjeneste.finnDifferanse(ytelseUke1OgNesteMandag, ytelseUke1, Betalingsmottaker.BRUKER)).contains(new OppdragPostConditionTjeneste.TilkjentYtelseDifferanse(nesteMandag, null, -100));
    }
}
