package no.nav.foreldrepenger.domene.prosess;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;

import java.util.Optional;

public interface BeregningAPI {

    Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId);

}
