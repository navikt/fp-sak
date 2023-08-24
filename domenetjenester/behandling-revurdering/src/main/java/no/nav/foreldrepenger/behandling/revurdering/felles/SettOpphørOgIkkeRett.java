package no.nav.foreldrepenger.behandling.revurdering.felles;


import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

import java.util.List;

public class SettOpphørOgIkkeRett {

    private SettOpphørOgIkkeRett() {
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, Behandlingsresultat behandlingsresultat, Vedtaksbrev vedtaksbrev) {
        return RevurderingBehandlingsresultatutlederFelles.buildBehandlingsresultat(revurdering, behandlingsresultat,
            BehandlingResultatType.OPPHØR, RettenTil.HAR_IKKE_RETT_TIL_FP,
            vedtaksbrev, List.of(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER));
    }
}
