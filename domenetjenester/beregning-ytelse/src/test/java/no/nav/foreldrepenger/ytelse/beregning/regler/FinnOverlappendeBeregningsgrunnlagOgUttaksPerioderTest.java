package no.nav.foreldrepenger.ytelse.beregning.regler;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.Beregningsresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatRegelmodellMellomregning;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.UttakResultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;

public class FinnOverlappendeBeregningsgrunnlagOgUttaksPerioderTest {

    private final String orgnr = "123";
    private BeregningsgrunnlagPrArbeidsforhold prArbeidsforhold;
    private Arbeidsforhold arbeidsforhold;

    /*
     * For eksempler brukt i testene under se
     * https://confluence.adeo.no/display/MODNAV/27b.+Beregne+tilkjent+ytelse
     */

    @Test
    public void skal_gradere_deltiddstilling_eksempel_1() {
        // Arrange
        var redBrukersAndelPrÅr = 0;
        var redRefusjonPrÅr = 10000;
        var stillingsgrad = BigDecimal.valueOf(50);
        var nyArbeidstidProsent = 40;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(4000.0);
        var faktiskDagsatArbeidsgiver = getDagsats(2000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_2() {
        // Arrange
        var redBrukersAndelPrÅr = 1000;
        var redRefusjonPrÅr = 9000;
        var stillingsgrad = BigDecimal.valueOf(50);
        var nyArbeidstidProsent = 40;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(4200.0);
        var faktiskDagsatArbeidsgiver = getDagsats(1800.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_3() {
        // Arrange
        var redBrukersAndelPrÅr = 0;
        var redRefusjonPrÅr = 100000;
        var stillingsgrad = BigDecimal.valueOf(50);
        var nyArbeidstidProsent = 50;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(50000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_4() {
        // Arrange
        var redBrukersAndelPrÅr = 10000;
        var redRefusjonPrÅr = 90000;
        var stillingsgrad = BigDecimal.valueOf(50);
        var nyArbeidstidProsent = 50;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(50000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_5() {
        // Arrange
        var redBrukersAndelPrÅr = 0;
        var redRefusjonPrÅr = 100000;
        var stillingsgrad = BigDecimal.valueOf(50);
        var nyArbeidstidProsent = 60;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(40000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_6() {
        // Arrange
        var redBrukersAndelPrÅr = 10000;
        var redRefusjonPrÅr = 90000;
        var stillingsgrad = BigDecimal.valueOf(50);
        var nyArbeidstidProsent = 60;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(40000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_heltiddstilling_eksempel_7() {
        // Arrange
        var redBrukersAndelPrÅr = 0;
        var redRefusjonPrÅr = 100000;
        var stillingsgrad = BigDecimal.valueOf(100);
        var nyArbeidstidProsent = 50;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatArbeidsgiver = getDagsats(50000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(0L);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_heltiddstilling_eksempel_8() {
        // Arrange
        var redBrukersAndelPrÅr = 10000;
        var redRefusjonPrÅr = 90000;
        var stillingsgrad = BigDecimal.valueOf(100);
        var nyArbeidstidProsent = 50;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(5000.0);
        var faktiskDagsatArbeidsgiver = getDagsats(45000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_gradere_deltiddstilling_eksempel_9() {
        // Arrange
        var redBrukersAndelPrÅr = 100000;
        var redRefusjonPrÅr = 500000;
        var stillingsgrad = BigDecimal.valueOf(50);
        var nyArbeidstidProsent = 40;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var faktiskDagsatBruker = getDagsats(260000.0);
        var faktiskDagsatArbeidsgiver = getDagsats(100000.0);

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_ikke_gradere_fulltidsstilling_med_full_permisjon() {
        // Arrange
        var redBrukersAndelPrÅr = 100000;
        var redRefusjonPrÅr = 0;
        var stillingsgrad = BigDecimal.valueOf(100);
        var nyArbeidstidProsent = 0;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, false);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(1);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
    }

    @Test
    public void skal_gradere_status_SN() {
        // Arrange
        var redusertBrukersAndel = BigDecimal.valueOf(100000);
        var stillingsgrad = BigDecimal.valueOf(100);
        var utbetalingsgrad = 50;
        var dagsatsBruker = redusertBrukersAndel.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
        var redDagsatsBruker = getDagsats(0.50 * redusertBrukersAndel.doubleValue());
        var mellomregning = settOppGraderingScenarioForAndreStatuser(redusertBrukersAndel, stillingsgrad,
                utbetalingsgrad, AktivitetStatus.SN, true);
        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(1);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(redDagsatsBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(0).getUtbetalingsgrad()).isEqualByComparingTo(BigDecimal.valueOf(utbetalingsgrad));
        assertThat(andel.get(0).getStillingsprosent()).isEqualByComparingTo(stillingsgrad);
    }

    @Test
    public void skal_teste_SN_med_oppholdsperiode() {
        // Arrange
        var redusertBrukersAndel = BigDecimal.valueOf(100000);
        var mellomregning = settOppScenarioMedOppholdsperiodeForSN(redusertBrukersAndel);
        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).isEmpty();
    }

    @Test
    public void skal_teste_AT_med_oppholdsperiode() {
        // Arrange
        var redBrukersAndelPrÅr = 100000;
        var redRefusjonPrÅr = 0;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppScenarioMedOppholdsperiodeForAT(redBrukersAndelPrÅr, redRefusjonPrÅr,
                dagsatsBruker, dagsatsArbeidsgiver);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).isEmpty();
    }

    @Test
    public void skal_gradere_status_DP() {
        // Arrange
        var redusertBrukersAndel = BigDecimal.valueOf(100000);
        var stillingsgrad = BigDecimal.valueOf(100);
        var utbetalingsgrad = 66;
        var dagsatsBruker = redusertBrukersAndel.divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue();
        var redDagsatsBruker = getDagsats(0.66 * redusertBrukersAndel.doubleValue());
        var mellomregning = settOppGraderingScenarioForAndreStatuser(redusertBrukersAndel, stillingsgrad,
                utbetalingsgrad, AktivitetStatus.DP, true);
        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(1);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(redDagsatsBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
    }

    @Test
    public void gradering_når_gammel_stillingsprosent_er_0() {
        // Arrange
        var redBrukersAndelPrÅr = 260000;
        var redRefusjonPrÅr = 26000;
        var stillingsgrad = BigDecimal.ZERO;
        var nyArbeidstidProsent = 0;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr,
                stillingsgrad, nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(dagsatsBruker + dagsatsArbeidsgiver);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(0);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_bruke_utbetalingsgrad_når_ikke_gradering() {
        // Arrange
        var redBrukersAndelPrÅr = 260000;
        var redRefusjonPrÅr = 130000;
        var stillingsgrad = BigDecimal.ZERO;
        var nyArbeidstidProsent = 50; // Gir 50% utbetalingsgrad
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr,
                stillingsgrad, nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, false);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(dagsatsBruker / 2);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(dagsatsArbeidsgiver / 2);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    @Test
    public void skal_ikke_regne_overkompensasjon_ved_100_prosent_stilling() {
        // Arrange
        var redBrukersAndelPrÅr = 100000;
        var redRefusjonPrÅr = 500000;
        var stillingsgrad = BigDecimal.valueOf(100);
        var nyArbeidstidProsent = 40;
        var dagsatsBruker = getDagsats(redBrukersAndelPrÅr);
        var dagsatsArbeidsgiver = getDagsats(redRefusjonPrÅr);
        var mellomregning = settOppGraderingScenario(redBrukersAndelPrÅr, redRefusjonPrÅr, stillingsgrad,
                nyArbeidstidProsent, dagsatsBruker, dagsatsArbeidsgiver, true);

        // Act
        var regel = new FinnOverlappendeBeregningsgrunnlagOgUttaksPerioder();
        var evaluation = regel.evaluate(mellomregning);
        var sporing = EvaluationSerializer.asJson(evaluation);
        assertThat(sporing).isNotNull();

        // Assert
        var redusertForDekningsgradBruker = BigDecimal.valueOf(redBrukersAndelPrÅr).multiply(BigDecimal.valueOf(60).scaleByPowerOfTen(-2));
        var redusertForDekningsgradArbeidsgiver = BigDecimal.valueOf(redRefusjonPrÅr).multiply(BigDecimal.valueOf(60).scaleByPowerOfTen(-2));
        var faktiskDagsatBruker = getDagsats(redusertForDekningsgradBruker.intValue());
        var faktiskDagsatArbeidsgiver = getDagsats(redusertForDekningsgradArbeidsgiver.intValue());

        var andel = mellomregning.getOutput().getBeregningsresultatPerioder().get(0).getBeregningsresultatAndelList();
        assertThat(andel).hasSize(2);
        assertThat(andel.get(0).erBrukerMottaker()).isTrue();
        assertThat(andel.get(0).getDagsats()).isEqualTo(faktiskDagsatBruker);
        assertThat(andel.get(0).getDagsatsFraBg()).isEqualTo(dagsatsBruker);
        assertThat(andel.get(1).erBrukerMottaker()).isFalse();
        assertThat(andel.get(1).getDagsats()).isEqualTo(faktiskDagsatArbeidsgiver);
        assertThat(andel.get(1).getDagsatsFraBg()).isEqualTo(dagsatsArbeidsgiver);
    }

    private BeregningsresultatRegelmodellMellomregning lagMellomregning(AktivitetStatus aktivitetStatus, BigDecimal stillingsgrad,
            BigDecimal arbeidstidsprosent, BigDecimal utbetalingsgrad, BigDecimal redusertBrukersAndel, boolean erGradering) {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);

        var grunnlag = lagBeregningsgrunnlag(fom, tom, aktivitetStatus, redusertBrukersAndel);
        var uttakResultat = new UttakResultat(
                lagUttakResultatPeriode(fom, tom, stillingsgrad, arbeidstidsprosent, utbetalingsgrad, aktivitetStatus, erGradering));
        var input = new BeregningsresultatRegelmodell(grunnlag, uttakResultat);
        var output = new Beregningsresultat();
        return new BeregningsresultatRegelmodellMellomregning(input, output);
    }

    private BeregningsresultatRegelmodellMellomregning lagMellomregningForOppholdsPeriode(AktivitetStatus aktivitetStatus,
            BigDecimal redusertBrukersAndel) {
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(14);

        var grunnlag = lagBeregningsgrunnlag(fom, tom, aktivitetStatus, redusertBrukersAndel);
        var uttakResultat = new UttakResultat(lagUttakResultatForOppholdsPeriode(fom, tom));
        var input = new BeregningsresultatRegelmodell(grunnlag, uttakResultat);
        var output = new Beregningsresultat();
        return new BeregningsresultatRegelmodellMellomregning(input, output);
    }

    private List<UttakResultatPeriode> lagUttakResultatPeriode(LocalDate fom, LocalDate tom, BigDecimal stillingsgrad, BigDecimal arbeidstidsprosent,
            BigDecimal utbetalingsgrad, AktivitetStatus aktivitetStatus, boolean erGradering) {

        var uttakAktiviter = Collections.singletonList(
                new UttakAktivitet(stillingsgrad, arbeidstidsprosent, utbetalingsgrad, arbeidsforhold, aktivitetStatus, erGradering, stillingsgrad));
        var periode = new UttakResultatPeriode(fom, tom, uttakAktiviter, false);
        return List.of(periode);
    }

    private List<UttakResultatPeriode> lagUttakResultatForOppholdsPeriode(LocalDate fom, LocalDate tom) {

        var periode = new UttakResultatPeriode(fom, tom, Collections.emptyList(), true);
        return List.of(periode);
    }

    private Beregningsgrunnlag lagBeregningsgrunnlag(LocalDate fom, LocalDate tom, AktivitetStatus aktivitetStatus, BigDecimal redusertBrukersAndel) {

        var periode1 = lagPeriode(fom, tom, aktivitetStatus, redusertBrukersAndel);

        return Beregningsgrunnlag.builder()
                .medSkjæringstidspunkt(LocalDate.now())
                .medAktivitetStatuser(Collections.singletonList(aktivitetStatus))
                .medBeregningsgrunnlagPeriode(periode1)
                .build();

    }

    private BeregningsgrunnlagPeriode lagPeriode(LocalDate fom, LocalDate tom, AktivitetStatus aktivitetStatus, BigDecimal redusertBrukersAndel) {
        var periode = new Periode(fom, tom);
        var builder = BeregningsgrunnlagPrStatus.builder()
                .medAktivitetStatus(aktivitetStatus)
                .medRedusertBrukersAndelPrÅr(redusertBrukersAndel);
        if (AktivitetStatus.ATFL.equals(aktivitetStatus)) {
            builder.medArbeidsforhold(prArbeidsforhold);
        }

        var bgPrStatus = builder.build();
        return BeregningsgrunnlagPeriode.builder().medPeriode(periode).medBeregningsgrunnlagPrStatus(bgPrStatus).build();
    }

    private BeregningsresultatRegelmodellMellomregning settOppGraderingScenario(int redBrukersAndelPrÅr, int redRefusjonPrÅr,
            BigDecimal stillingsgrad,
            int nyArbeidstidProsent, long dagsatsBruker, Long dagsatsArbeidsgiver, boolean erGradering) {
        arbeidsforhold = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr);
        prArbeidsforhold = BeregningsgrunnlagPrArbeidsforhold.builder().medArbeidsforhold(arbeidsforhold)
                .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(redBrukersAndelPrÅr))
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(redRefusjonPrÅr))
                .medDagsatsBruker(dagsatsBruker)
                .medDagsatsArbeidsgiver(dagsatsArbeidsgiver)
                .build();
        var utbetalingsgrad = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(nyArbeidstidProsent));
        return lagMellomregning(AktivitetStatus.ATFL, stillingsgrad, new BigDecimal(nyArbeidstidProsent), utbetalingsgrad,
                BigDecimal.valueOf(redBrukersAndelPrÅr), erGradering);
    }

    private static long getDagsats(int årsbeløp) {
        return Math.round(årsbeløp / 260.0);
    }

    private static long getDagsats(double årsbeløp) {
        return Math.round(årsbeløp / 260.0);
    }

    private BeregningsresultatRegelmodellMellomregning settOppGraderingScenarioForAndreStatuser(BigDecimal redusertBrukersAndel,
            BigDecimal stillingsgrad, int utbetalingsgrad,
            AktivitetStatus aktivitetStatus, boolean erGradering) {
        return lagMellomregning(aktivitetStatus, stillingsgrad, BigDecimal.ZERO, BigDecimal.valueOf(utbetalingsgrad), redusertBrukersAndel,
                erGradering);
    }

    private BeregningsresultatRegelmodellMellomregning settOppScenarioMedOppholdsperiodeForSN(BigDecimal redusertBrukersAndel) {
        return lagMellomregningForOppholdsPeriode(AktivitetStatus.SN, redusertBrukersAndel);
    }

    private BeregningsresultatRegelmodellMellomregning settOppScenarioMedOppholdsperiodeForAT(int redBrukersAndelPrÅr, int redRefusjonPrÅr,
            long dagsatsBruker, Long dagsatsArbeidsgiver) {
        arbeidsforhold = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr);
        prArbeidsforhold = BeregningsgrunnlagPrArbeidsforhold.builder().medArbeidsforhold(arbeidsforhold)
                .medRedusertBrukersAndelPrÅr(BigDecimal.valueOf(redBrukersAndelPrÅr))
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(redRefusjonPrÅr))
                .medDagsatsBruker(dagsatsBruker)
                .medDagsatsArbeidsgiver(dagsatsArbeidsgiver)
                .build();
        return lagMellomregningForOppholdsPeriode(AktivitetStatus.ATFL, BigDecimal.valueOf(redBrukersAndelPrÅr));
    }

}
