package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BeregningsresultatAndel {

    private Boolean brukerErMottaker;
    private Arbeidsforhold arbeidsforhold;
    private Long dagsats;
    private BigDecimal stillingsprosent = BigDecimal.valueOf(100);
    private BigDecimal utbetalingsgrad = BigDecimal.valueOf(100);
    private Long dagsatsFraBg;
    private AktivitetStatus aktivitetStatus;
    private Inntektskategori inntektskategori;
    private List<BeregningsresultatFeriepengerPrÅr> beregningsresultatFeriepengerPrÅrListe = new ArrayList<>();

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

    public List<BeregningsresultatFeriepengerPrÅr> getBeregningsresultatFeriepengerPrÅrListe() {
        return beregningsresultatFeriepengerPrÅrListe;
    }

    public void addBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepengerPrÅr beregningsresultatFeriepengerPrÅr) {
        Objects.requireNonNull(beregningsresultatFeriepengerPrÅr, "beregningsresultatFeriepengerPrÅr");
        if (!beregningsresultatFeriepengerPrÅrListe.contains(beregningsresultatFeriepengerPrÅr)) {
            beregningsresultatFeriepengerPrÅrListe.add(beregningsresultatFeriepengerPrÅr);
        }
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

    public static BeregningsresultatAndel copyUtenFeriepenger(BeregningsresultatAndel andel) {
        return BeregningsresultatAndel.builder()
            .medAktivitetStatus(andel.aktivitetStatus)
            .medArbeidsforhold(andel.arbeidsforhold)
            .medBrukerErMottaker(andel.brukerErMottaker)
            .medDagsats(andel.dagsats)
            .medDagsatsFraBg(andel.dagsatsFraBg)
            .medInntektskategori(andel.inntektskategori)
            .medStillingsprosent(andel.stillingsprosent)
            .medUtbetalingssgrad(andel.utbetalingsgrad)
            .build();
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
