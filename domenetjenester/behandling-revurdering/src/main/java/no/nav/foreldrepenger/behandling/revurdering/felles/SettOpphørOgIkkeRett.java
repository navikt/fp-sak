package no.nav.foreldrepenger.behandling.revurdering.felles;


import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

public class SettOpphørOgIkkeRett {

    private SettOpphørOgIkkeRett() {
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, Behandlingsresultat behandlingsresultat, Vedtaksbrev vedtaksbrev) {
        return RevurderingBehandlingsresultatutlederFelles.buildBehandlingsresultat(revurdering, behandlingsresultat,
            BehandlingResultatType.OPPHØR, RettenTil.HAR_IKKE_RETT_TIL_FP,
            vedtaksbrev, List.of(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER));
    }
}
