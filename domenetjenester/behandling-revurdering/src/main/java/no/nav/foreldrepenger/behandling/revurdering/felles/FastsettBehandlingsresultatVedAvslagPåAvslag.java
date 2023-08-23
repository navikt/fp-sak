package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

import java.util.List;
import java.util.Optional;

class FastsettBehandlingsresultatVedAvslagPåAvslag {

    private FastsettBehandlingsresultatVedAvslagPåAvslag() {
    }

    static boolean vurder(Optional<Behandlingsresultat> resRevurdering, Optional<Behandlingsresultat> resOriginal, BehandlingType originalType) {
        return resOriginal.isPresent() && resRevurdering.isPresent() && BehandlingType.FØRSTEGANGSSØKNAD.equals(originalType) && erAvslagPåAvslag(
            resRevurdering.get(), resOriginal.get());
    }

    static Behandlingsresultat fastsett(Behandling revurdering, Behandlingsresultat behandlingsresultat) {
        return RevurderingBehandlingsresultatutlederFelles.buildBehandlingsresultat(revurdering, behandlingsresultat,
            BehandlingResultatType.INGEN_ENDRING, RettenTil.HAR_RETT_TIL_FP, Vedtaksbrev.INGEN, List.of(KonsekvensForYtelsen.INGEN_ENDRING));
    }

    private static boolean erAvslagPåAvslag(Behandlingsresultat resRevurdering, Behandlingsresultat resOriginal) {
        return resRevurdering.isVilkårAvslått() && resOriginal.isBehandlingsresultatAvslått();
    }
}
