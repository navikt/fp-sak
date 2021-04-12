package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;

import java.util.Optional;

public interface BehandlingsresultatBeregningTjeneste {

    boolean erUgunst(BehandlingReferanse ref);

    boolean erEndring(BehandlingReferanse ref);
}

