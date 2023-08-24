package no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

import java.math.BigDecimal;

public record UttakAktivitet(BigDecimal stillingsgrad,
                             BigDecimal arbeidstidsprosent,
                             BigDecimal utbetalingsgrad,
                             boolean reduserDagsatsMedUtbetalingsgrad,
                             Arbeidsforhold arbeidsforhold,
                             AktivitetStatus aktivitetStatus,
                             boolean erGradering,
                             BigDecimal totalStillingsgradHosAG) {

    public static UttakAktivitet ny(AktivitetStatus aktivitetStatus, BigDecimal utbetalingsgrad, boolean reduserDagsatsMedUtbetalingsgrad) {
        return new UttakAktivitet(null, null, utbetalingsgrad, reduserDagsatsMedUtbetalingsgrad, null, aktivitetStatus, false, null);
    }

    public UttakAktivitet medArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        return new UttakAktivitet(stillingsgrad(), arbeidstidsprosent(), utbetalingsgrad(), reduserDagsatsMedUtbetalingsgrad(), arbeidsforhold, aktivitetStatus(), erGradering(), totalStillingsgradHosAG());
    }

    public UttakAktivitet medStillingsgrad(BigDecimal stillingsgrad, BigDecimal totalStillingsgradHosAG) {
        return new UttakAktivitet(stillingsgrad, arbeidstidsprosent(), utbetalingsgrad(), reduserDagsatsMedUtbetalingsgrad(), arbeidsforhold(), aktivitetStatus(), erGradering(), totalStillingsgradHosAG);
    }

    public UttakAktivitet medGradering(boolean erGradering, BigDecimal arbeidstidsprosent) {
        return new UttakAktivitet(stillingsgrad(), arbeidstidsprosent, utbetalingsgrad(), reduserDagsatsMedUtbetalingsgrad(), arbeidsforhold(), aktivitetStatus(), erGradering, totalStillingsgradHosAG());
    }

}
