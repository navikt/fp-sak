package no.nav.foreldrepenger.ytelse.beregning.regelmodell;

import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Dekningsgrad;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public class BeregningsresultatFeriepengerGrunnlag {
    private boolean arbeidstakerVedSkjæringstidspunkt;
    private Set<Inntektskategori> inntektskategorier;
    private List<BeregningsresultatPeriode> beregningsresultatPerioder;
    private Dekningsgrad dekningsgrad;
    private Set<Inntektskategori> inntektskategorierAnnenPart;
    private boolean erForelder1;
    private List<BeregningsresultatPeriode> annenPartsBeregningsresultatPerioder;
    private int antallDagerFeriepenger;


    private BeregningsresultatFeriepengerGrunnlag() {
        //tom konstruktør
    }

    public boolean erArbeidstakerVedSkjæringstidspunkt() {
        return arbeidstakerVedSkjæringstidspunkt;
    }

    public Set<Inntektskategori> getInntektskategorier() {
        return inntektskategorier;
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return beregningsresultatPerioder;
    }

    public List<BeregningsresultatPeriode> getAnnenPartsBeregningsresultatPerioder() {
        return annenPartsBeregningsresultatPerioder;
    }

    public Set<Inntektskategori> getInntektskategorierAnnenPart() {
        return inntektskategorierAnnenPart;
    }

    public Dekningsgrad getDekningsgrad() {
        return dekningsgrad;
    }

    public boolean erForelder1() {
        return erForelder1;
    }

    public int getAntallDagerFeriepenger() {
        return antallDagerFeriepenger;
    }

    public static Builder builder(BeregningsresultatFeriepengerGrunnlag regelModell) {
        return new Builder(regelModell);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningsresultatFeriepengerGrunnlag kladd;

        private Builder(BeregningsresultatFeriepengerGrunnlag regelModell) {
            kladd = regelModell;
        }

        public Builder() {
            this.kladd = new BeregningsresultatFeriepengerGrunnlag();
        }

        public Builder medArbeidstakerVedSkjæringstidspunkt(boolean arbeidstakerVedSkjæringstidspunkt) {
            kladd.arbeidstakerVedSkjæringstidspunkt = arbeidstakerVedSkjæringstidspunkt;
            return this;
        }

        public Builder medInntektskategorier(Set<Inntektskategori> inntektskategorier) {
            kladd.inntektskategorier = inntektskategorier;
            return this;
        }

        public Builder medBeregningsresultatPerioder(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
            kladd.beregningsresultatPerioder = beregningsresultatPerioder;
            return this;
        }

        public Builder medAnnenPartsBeregningsresultatPerioder(List<BeregningsresultatPeriode> annenPartsBeregningsresultatPerioder) {
            kladd.annenPartsBeregningsresultatPerioder = annenPartsBeregningsresultatPerioder;
            return this;
        }

        public Builder medDekningsgrad(Dekningsgrad dekningsgrad) {
            kladd.dekningsgrad = dekningsgrad;
            return this;
        }

        public Builder medAnnenPartsInntektskategorier(Set<Inntektskategori> inntektskategorierAnnenPart) {
            kladd.inntektskategorierAnnenPart = inntektskategorierAnnenPart;
            return this;
        }

        public Builder medErForelder1(boolean erForelder1) {
            kladd.erForelder1 = erForelder1;
            return this;
        }

        public Builder medAntallDagerFeriepenger(int antallDagerFeriepenger) {
            kladd.antallDagerFeriepenger = antallDagerFeriepenger;
            return this;
        }

        public BeregningsresultatFeriepengerGrunnlag build() {
            return kladd;
        }
    }
}
