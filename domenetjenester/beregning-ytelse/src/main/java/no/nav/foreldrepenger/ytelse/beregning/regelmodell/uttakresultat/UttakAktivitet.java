package no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat;

import java.math.BigDecimal;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public record UttakAktivitet(BigDecimal stillingsgrad,
                             BigDecimal arbeidstidsprosent,
                             BigDecimal utbetalingsgrad,
                             Arbeidsforhold arbeidsforhold,
                             AktivitetStatus aktivitetStatus,
                             boolean erGradering,
                             BigDecimal totalStillingsgradHosAG) {

    public static UttakAktivitet ny(AktivitetStatus aktivitetStatus) {
        return new UttakAktivitet(null, null, null, null, aktivitetStatus, false, null);
    }

    public UttakAktivitet medArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        return new UttakAktivitet(stillingsgrad(), arbeidstidsprosent(), utbetalingsgrad(), arbeidsforhold, aktivitetStatus(), erGradering(), totalStillingsgradHosAG());
    }

    public UttakAktivitet medStillingsgrad(BigDecimal stillingsgrad, BigDecimal totalStillingsgradHosAG) {
        return new UttakAktivitet(stillingsgrad, arbeidstidsprosent(), utbetalingsgrad(), arbeidsforhold(), aktivitetStatus(), erGradering(), totalStillingsgradHosAG);
    }

    public UttakAktivitet medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
        return new UttakAktivitet(stillingsgrad(), arbeidstidsprosent(), utbetalingsgrad, arbeidsforhold(), aktivitetStatus(), erGradering(), totalStillingsgradHosAG());
    }

    public UttakAktivitet medGradering(boolean erGradering, BigDecimal arbeidstidsprosent) {
        return new UttakAktivitet(stillingsgrad(), arbeidstidsprosent, utbetalingsgrad(), arbeidsforhold(), aktivitetStatus(), erGradering, totalStillingsgradHosAG());
    }

}
