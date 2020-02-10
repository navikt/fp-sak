package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;

public interface RevurderingBehandlingsresultatutlederFelles {

    Behandlingsresultat bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurdering, boolean erVarselOmRevurderingSendt);
}
