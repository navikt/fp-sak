package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.math.BigDecimal;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public class UttakAktivitet {
    private BigDecimal stillingsgrad;
    private BigDecimal arbeidstidsprosent;
    private BigDecimal utbetalingsgrad;
    private Arbeidsforhold arbeidsforhold;
    private AktivitetStatus aktivitetStatus;
    private boolean erGradering;

    public UttakAktivitet(BigDecimal stillingsgrad, BigDecimal arbeidstidsprosent, BigDecimal utbetalingsgrad, Arbeidsforhold arbeidsforhold, AktivitetStatus aktivitetStatus, boolean erGradering) {
        this.stillingsgrad = stillingsgrad;
        this.arbeidstidsprosent = arbeidstidsprosent;
        this.utbetalingsgrad = utbetalingsgrad;
        this.arbeidsforhold = arbeidsforhold;
        this.aktivitetStatus = aktivitetStatus;
        this.erGradering = erGradering;
    }

    public BigDecimal getStillingsgrad() {
        return stillingsgrad;
    }

    public BigDecimal getArbeidstidsprosent() {
        return arbeidstidsprosent;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public boolean isErGradering() {
        return erGradering;
    }

    @Override
    public String toString() {
        return "UttakAktivitet{" +
            "stillingsgrad=" + stillingsgrad +
            ", arbeidstidsprosent=" + arbeidstidsprosent +
            ", utbetalingsgrad=" + utbetalingsgrad +
            ", arbeidsforhold=" + arbeidsforhold +
            ", aktivitetStatus=" + aktivitetStatus +
            ", erGradering=" + erGradering +
            '}';
    }
}
