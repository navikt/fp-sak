package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public class BeregningsresultatFeriepengerPrÅr {

    private LocalDate opptjeningÅr;
    private BigDecimal årsbeløp;
    private Boolean brukerErMottaker;
    private Arbeidsforhold arbeidsforhold;
    private AktivitetStatus aktivitetStatus;

    private BeregningsresultatFeriepengerPrÅr() {
    }

    public LocalDate getOpptjeningÅr() {
        return opptjeningÅr;
    }

    public BigDecimal getÅrsbeløp() {
        return årsbeløp;
    }

    public Boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public String getArbeidsgiverId() {
        return arbeidsforhold == null ? null : arbeidsforhold.identifikator();
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatFeriepengerPrÅr eksisterende) {
        return new Builder(eksisterende);
    }

    public static class Builder {

        private BeregningsresultatFeriepengerPrÅr kladd;

        public Builder() {
            kladd = new BeregningsresultatFeriepengerPrÅr();
        }

        public Builder(BeregningsresultatFeriepengerPrÅr kladd) {
            this.kladd = kladd;
        }

        public Builder medOpptjeningÅr(LocalDate opptjeningÅr) {
            kladd.opptjeningÅr = opptjeningÅr;
            return this;
        }

        public Builder medÅrsbeløp(BigDecimal årsbeløp) {
            kladd.årsbeløp = årsbeløp;
            return this;
        }

        public Builder medBrukerErMottaker(boolean brukerErMottaker) {
            kladd.brukerErMottaker = brukerErMottaker;
            return this;
        }

        public Builder medArbeidsforhold(Arbeidsforhold arbeidsforhold) {
            kladd.arbeidsforhold = arbeidsforhold;
            return this;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            kladd.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public BeregningsresultatFeriepengerPrÅr build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.opptjeningÅr, "opptjeningÅr");
            Objects.requireNonNull(kladd.årsbeløp, "årsbeløp");
            Objects.requireNonNull(kladd.brukerErMottaker, "brukerErMottaker");
            Objects.requireNonNull(kladd.aktivitetStatus, "aktivitetStatus");
        }
    }
}
