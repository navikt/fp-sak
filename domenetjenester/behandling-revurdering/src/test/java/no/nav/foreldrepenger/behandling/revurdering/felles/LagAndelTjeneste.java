package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;

import java.util.List;

public interface LagAndelTjeneste {

    List<BeregningsgrunnlagPrStatusOgAndel> lagAndeler(boolean medOppjustertDagsat,
                                                       boolean skalDeleAndelMellomArbeidsgiverOgBruker);

}
