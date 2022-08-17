package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.util.List;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;

public interface LagAndelTjeneste {

    List<BeregningsgrunnlagPrStatusOgAndel> lagAndeler(boolean medOppjustertDagsat,
                                                       boolean skalDeleAndelMellomArbeidsgiverOgBruker);

}
