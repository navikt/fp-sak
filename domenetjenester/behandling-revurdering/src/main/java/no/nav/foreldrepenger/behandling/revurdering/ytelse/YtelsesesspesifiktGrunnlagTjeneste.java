package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;

public interface YtelsesesspesifiktGrunnlagTjeneste {

    Optional<? extends YtelsespesifiktGrunnlag> grunnlag(BehandlingReferanse ref);
}
