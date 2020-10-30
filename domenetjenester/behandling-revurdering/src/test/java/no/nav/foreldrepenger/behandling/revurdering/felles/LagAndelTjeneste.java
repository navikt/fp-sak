package no.nav.foreldrepenger.behandling.revurdering.felles;


import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;

public interface LagAndelTjeneste {

    void lagAndeler(BeregningsgrunnlagPeriode periode,
                           boolean medOppjustertDagsat,
                           boolean skalDeleAndelMellomArbeidsgiverOgBruker);

}
