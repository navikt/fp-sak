package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.List;

public class BeregningsgrunnlagEndring {

    private final List<BeregningsgrunnlagPeriodeEndring> beregningsgrunnlagPeriodeEndringer;

    public BeregningsgrunnlagEndring(List<BeregningsgrunnlagPeriodeEndring> beregningsgrunnlagPeriodeEndringer) {
        this.beregningsgrunnlagPeriodeEndringer = beregningsgrunnlagPeriodeEndringer;
    }

    public List<BeregningsgrunnlagPeriodeEndring> getBeregningsgrunnlagPeriodeEndringer() {
        return beregningsgrunnlagPeriodeEndringer;
    }
}
