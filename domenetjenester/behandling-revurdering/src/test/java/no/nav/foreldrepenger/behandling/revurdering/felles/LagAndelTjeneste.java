package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;

public interface LagAndelTjeneste {

    void lagAndeler(BeregningsgrunnlagPeriode periode,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker);

}
