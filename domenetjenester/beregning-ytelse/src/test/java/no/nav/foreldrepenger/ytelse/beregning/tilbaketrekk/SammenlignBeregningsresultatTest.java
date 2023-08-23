package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.*;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SammenlignBeregningsresultatTest {

    @Test
    void skal_ikke_finne_diff_når_to_entiteter_er_like() {
        var stp = LocalDate.now();
        var builder1 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        var beregningsresultat1 = builder1.build();
        var brPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp.minusDays(20), stp.minusDays(15))
            .build(beregningsresultat1);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(brPeriode1);

        var builder2 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        var beregningsresultat2 = builder2.build();
        var brPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp.minusDays(20), stp.minusDays(15))
            .build(beregningsresultat2);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(brPeriode2);

        var erLike = SammenlignBeregningsresultat.erLike(beregningsresultat1, beregningsresultat2);

        assertThat(erLike).isTrue();
    }

    @Test
    void skal_finne_diff_når_to_entiteter_har_ulike_start_på_periode() {
        var stp = LocalDate.now();
        var builder1 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        var beregningsresultat1 = builder1.build();
        var brPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp.minusDays(21), stp.minusDays(15)) // ulik
            .build(beregningsresultat1);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(brPeriode1);

        var builder2 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        var beregningsresultat2 = builder2.build();
        var brPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp.minusDays(20), stp.minusDays(15))
            .build(beregningsresultat2);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(brPeriode2);

        var erLike = SammenlignBeregningsresultat.erLike(beregningsresultat1, beregningsresultat2);

        assertThat(erLike).isFalse();
    }

    @Test
    void skal_finne_diff_når_to_entiteter_har_ulike_utbetalingsgrad_på_andel() {
        var stp = LocalDate.now();
        var builder1 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        var beregningsresultat1 = builder1.build();
        var brPeriode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp.minusDays(21), stp.minusDays(15))
            .build(beregningsresultat1);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(99)) // ulik
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(brPeriode1);

        var builder2 = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2");
        var beregningsresultat2 = builder2.build();
        var brPeriode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp.minusDays(20), stp.minusDays(15))
            .build(beregningsresultat2);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medDagsats(2160)
            .medDagsatsFraBg(2160)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(brPeriode2);

        var erLike = SammenlignBeregningsresultat.erLike(beregningsresultat1, beregningsresultat2);

        assertThat(erLike).isFalse();
    }
}
