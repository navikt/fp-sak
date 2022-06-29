package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

public interface BeregningAPI {

    Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId);

}
