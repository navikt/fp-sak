package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.List;

import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;

public class BeregningsgrunnlagPeriodeEndring {

    private final List<BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer;

    private final Periode periode;


    public BeregningsgrunnlagPeriodeEndring(List<BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer, Periode periode) {
        this.beregningsgrunnlagPrStatusOgAndelEndringer = beregningsgrunnlagPrStatusOgAndelEndringer;
        this.periode = periode;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelEndring> getBeregningsgrunnlagPrStatusOgAndelEndringer() {
        return beregningsgrunnlagPrStatusOgAndelEndringer;
    }

    public Periode getPeriode() {
        return periode;
    }
}
