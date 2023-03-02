package no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.util.List;

public record Beregningsgrunnlag(List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder) {

    public Beregningsgrunnlag {
        if (beregningsgrunnlagPerioder == null || beregningsgrunnlagPerioder.isEmpty()) {
            throw new IllegalArgumentException("Ingen beregningsgrunnlagperioder");
        }
    }

    public static Beregningsgrunnlag enkelPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        return new Beregningsgrunnlag(List.of(beregningsgrunnlagPeriode));
    }
}
