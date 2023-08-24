package no.nav.foreldrepenger.domene.modell;

import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;

import java.util.Objects;

public class BeregningsgrunnlagFaktaOmBeregningTilfelle {

    private Beregningsgrunnlag beregningsgrunnlag;
    private FaktaOmBeregningTilfelle faktaOmBeregningTilfelle = FaktaOmBeregningTilfelle.UDEFINERT;
    public FaktaOmBeregningTilfelle getFaktaOmBeregningTilfelle() {
        return faktaOmBeregningTilfelle;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BeregningsgrunnlagFaktaOmBeregningTilfelle that)) {
            return false;
        }
        return Objects.equals(beregningsgrunnlag, that.beregningsgrunnlag) &&
                Objects.equals(faktaOmBeregningTilfelle, that.faktaOmBeregningTilfelle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregningsgrunnlag, faktaOmBeregningTilfelle);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsgrunnlagFaktaOmBeregningTilfelle beregningsgrunnlagFaktaOmBeregningTilfelle;

        public Builder() {
            beregningsgrunnlagFaktaOmBeregningTilfelle = new BeregningsgrunnlagFaktaOmBeregningTilfelle();
        }

        Builder medFaktaOmBeregningTilfelle(FaktaOmBeregningTilfelle tilfelle) {
            beregningsgrunnlagFaktaOmBeregningTilfelle.faktaOmBeregningTilfelle = tilfelle;
            return this;
        }

        public BeregningsgrunnlagFaktaOmBeregningTilfelle build(Beregningsgrunnlag beregningsgrunnlag) {
            beregningsgrunnlagFaktaOmBeregningTilfelle.beregningsgrunnlag = beregningsgrunnlag;
            return beregningsgrunnlagFaktaOmBeregningTilfelle;
        }
    }
}
