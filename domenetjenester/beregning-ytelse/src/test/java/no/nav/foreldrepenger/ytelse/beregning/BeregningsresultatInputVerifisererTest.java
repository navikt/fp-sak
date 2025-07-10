package no.nav.foreldrepenger.ytelse.beregning;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatGrunnlag;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakAktivitet;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat.UttakResultatPeriode;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class BeregningsresultatInputVerifisererTest {
    private List<BeregningsgrunnlagPeriode> bgPerioder = new ArrayList<>();
    private List<UttakResultatPeriode> uttakPerioder = new ArrayList<>();

    @Test
    void skal_teste_at_uttak_andeler_kun_valideres_mot_bg_andeler_i_sin_egen_periode() {
        var arbfor1 = lagBGArbeidsforhold("999999999", null, false);
        var arbfor2 = lagBGArbeidsforhold("999999998", null, false);
        lagBGPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), arbfor1);
        lagBGPeriode(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 11), arbfor1, arbfor2);
        lagBGPeriode(LocalDate.of(2020, 9, 12), LocalDate.of(2020, 10, 21), arbfor1, arbfor2);
        lagBGPeriode(LocalDate.of(2020, 10, 22), LocalDateInterval.TIDENES_ENDE, arbfor1, arbfor2);
        var uttakAktivitet = lagUttakAktivitet("999999999", null, false);
        var uttakAktivitet2 = lagUttakAktivitet("999999998", null, false);
        lagUttakPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 10, 21), uttakAktivitet);
        lagUttakPeriode(LocalDate.of(2020, 9, 1), LocalDate.of(2020, 9, 11), uttakAktivitet2);
        lagUttakPeriode(LocalDate.of(2020, 9, 12), LocalDate.of(2020, 10, 21), uttakAktivitet2);

        var uttakResultat = new UttakResultat(uttakPerioder);
        var input = new BeregningsresultatGrunnlag(new Beregningsgrunnlag(bgPerioder), uttakResultat);
        assertDoesNotThrow(() -> BeregningsresultatInputVerifiserer.verifiserAndelerIUttakLiggerIBeregning(input));
    }

    @Test
    void skal_ikke_validere_andeler_som_ligger_i_perioder_med_fom_etter_siste_uttaksdag() {
        var arbfor1 = lagBGArbeidsforhold("999999999", null, false);
        var arbfor2 = lagBGArbeidsforhold("999999998", null, false);
        lagBGPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), arbfor1);
        lagBGPeriode(LocalDate.of(2020, 9, 1), LocalDate.of(9999, 12, 31), arbfor1, arbfor2);
        var uttakAktivitet = lagUttakAktivitet("999999999", null, false);
        lagUttakPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), uttakAktivitet);

        var uttakResultat = new UttakResultat(uttakPerioder);
        var input = new BeregningsresultatGrunnlag(new Beregningsgrunnlag(bgPerioder), uttakResultat);
        assertDoesNotThrow(() -> BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(input));
    }

    @Test
    void en_periode_skal_matches_med_uttak() {
        var arbfor1 = lagBGArbeidsforhold("999999999", null, false);
        lagBGPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(9999, 12, 31), arbfor1);
        var uttakAktivitet = lagUttakAktivitet("999999999", null, false);
        lagUttakPeriode(LocalDate.of(2020, 8, 3), LocalDate.of(2020, 8, 31), uttakAktivitet);

        var uttakResultat = new UttakResultat(uttakPerioder);
        var input = new BeregningsresultatGrunnlag(new Beregningsgrunnlag(bgPerioder), uttakResultat);
        assertDoesNotThrow(() -> BeregningsresultatInputVerifiserer.verifiserAlleAndelerIBeregningErIUttak(input));
    }


    private BeregningsgrunnlagPrArbeidsforhold lagBGArbeidsforhold(String orgnr, String referanse, boolean erFrilans) {
        var arbeidsforhold = erFrilans ? Arbeidsforhold.frilansArbeidsforhold()
                : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr, referanse);
        return new BeregningsgrunnlagPrArbeidsforhold(arbeidsforhold, null, null, Inntektskategori.ARBEIDSTAKER);
    }

    private void lagBGPeriode(LocalDate fom, LocalDate tom, BeregningsgrunnlagPrArbeidsforhold... arbfor) {
        var bgAndelArbfor = Arrays.asList(arbfor);
        var periode = new BeregningsgrunnlagPeriode(fom, tom, List.of(new BeregningsgrunnlagPrStatus(AktivitetStatus.ATFL, bgAndelArbfor)));
        bgPerioder.add(periode);
    }

    private void lagUttakPeriode(LocalDate fom, LocalDate tom, UttakAktivitet... andeler) {
        uttakPerioder.add(new UttakResultatPeriode(fom, tom, Arrays.asList(andeler), false));
    }

    private UttakAktivitet lagUttakAktivitet(String orgnr, String referanse, boolean erFrilans) {
        var arbeidsforhold = erFrilans ? Arbeidsforhold.frilansArbeidsforhold()
                : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr, referanse);
        return UttakAktivitet.ny(AktivitetStatus.ATFL, BigDecimal.valueOf(100), true)
            .medArbeidsforhold(arbeidsforhold)
            .medStillingsgrad( BigDecimal.valueOf(100),  BigDecimal.valueOf(100))
            .medGradering(false, BigDecimal.ZERO);
    }
}
