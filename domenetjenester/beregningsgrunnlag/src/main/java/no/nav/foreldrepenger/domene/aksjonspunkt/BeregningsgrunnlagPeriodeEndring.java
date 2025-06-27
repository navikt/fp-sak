package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.List;

import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class BeregningsgrunnlagPeriodeEndring {

    private final List<BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer;

    private final DatoIntervallEntitet periode;


    public BeregningsgrunnlagPeriodeEndring(List<BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer, DatoIntervallEntitet periode) {
        this.beregningsgrunnlagPrStatusOgAndelEndringer = beregningsgrunnlagPrStatusOgAndelEndringer;
        this.periode = periode;
    }

    public List<BeregningsgrunnlagPrStatusOgAndelEndring> getBeregningsgrunnlagPrStatusOgAndelEndringer() {
        return beregningsgrunnlagPrStatusOgAndelEndringer;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }
}
