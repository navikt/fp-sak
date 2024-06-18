package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.math.BigDecimal;
import java.util.Objects;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;

public class BeregningsresultatAndel {

    private Boolean brukerErMottaker;
    private Arbeidsforhold arbeidsforhold;
    private Long dagsats;
    private BigDecimal stillingsprosent = BigDecimal.valueOf(100);
    private BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);
    private Long dagsatsFraBg;
    private AktivitetStatus aktivitetStatus;
    private Inntektskategori inntektskategori;

    private BeregningsresultatAndel() {
    }


    public Boolean erBrukerMottaker() {
        return brukerErMottaker;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public Long getDagsatsFraBg() {
        return dagsatsFraBg;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Arbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public String getArbeidsgiverId() {
        return arbeidsforhold == null ? null : arbeidsforhold.identifikator();
    }

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    @Override
    public String toString() {
        return "BeregningsresultatAndel{" +
            "aktivitetStatus='" + aktivitetStatus.name() + '\'' +
            ", arbeidsgiverId=" + (arbeidsforhold != null ? arbeidsforhold.identifikator() : null) +
            ", arbeidsforholdId=" + (arbeidsforhold != null ? arbeidsforhold.arbeidsforholdId() : null) +
            ", erBrukerMottaker=" + erBrukerMottaker() +
            '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningsresultatAndel beregningsresultatAndelMal;

        public Builder() {
            beregningsresultatAndelMal = new BeregningsresultatAndel();
        }

        public Builder medBrukerErMottaker(Boolean brukerErMottaker) {
            beregningsresultatAndelMal.brukerErMottaker = brukerErMottaker;
            return this;
        }

        public Builder medArbeidsforhold(Arbeidsforhold arbeidsforhold) {
            beregningsresultatAndelMal.arbeidsforhold = arbeidsforhold;
            return this;
        }

        public Builder medDagsats(Long dagsats) {
            beregningsresultatAndelMal.dagsats = dagsats;
            return this;
        }

        public Builder medStillingsprosent(BigDecimal stillingsprosent) {
            beregningsresultatAndelMal.stillingsprosent = stillingsprosent;
            return this;
        }

        public Builder medUtbetalingssgrad(BigDecimal utbetalingsgrad) {
            beregningsresultatAndelMal.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medDagsatsFraBg(Long dagsatsFraBg) {
            beregningsresultatAndelMal.dagsatsFraBg = dagsatsFraBg;
            return this;
        }

        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            beregningsresultatAndelMal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medInntektskategori(Inntektskategori inntektskategori) {
            beregningsresultatAndelMal.inntektskategori = inntektskategori;
            return this;
        }

        public BeregningsresultatAndel build() {
            verifyStateForBuild();
            return beregningsresultatAndelMal;
        }

        void verifyStateForBuild() {
            Objects.requireNonNull(beregningsresultatAndelMal.brukerErMottaker, "brukerErMottaker");
            Objects.requireNonNull(beregningsresultatAndelMal.dagsats, "dagsats");
            Objects.requireNonNull(beregningsresultatAndelMal.dagsatsFraBg, "dagsatsFraBg");
            Objects.requireNonNull(beregningsresultatAndelMal.utbetalingsgrad, "utbetalingsgrad");
        }
    }
}
