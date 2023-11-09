package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;

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
