package no.nav.foreldrepenger.behandling.revurdering;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;

public abstract class RevurderingEndringBasertPåKonsekvenserForYtelsen implements RevurderingEndring {

    public static final String UTVIKLERFEIL_INGEN_ENDRING_SAMMEN = "Utviklerfeil: Det skal ikke være mulig å ha INGEN_ENDRING sammen med andre konsekvenser. BehandlingId: ";

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling, BehandlingResultatType nyResultatType) {
        return erRevurderingMedUendretUtfall(behandling);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        if (!BehandlingType.REVURDERING.equals(behandling.getType())) {
            return false;
        }
        var behandlingsresultat = behandling.getBehandlingsresultat();
        var konsekvenserForYtelsen = behandlingsresultat.getKonsekvenserForYtelsen();
        var ingenKonsekvensForYtelsen = konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING);
        if (ingenKonsekvensForYtelsen && konsekvenserForYtelsen.size() > 1) {
            throw new IllegalStateException(UTVIKLERFEIL_INGEN_ENDRING_SAMMEN + behandling.getId());
        }
        return ingenKonsekvensForYtelsen;
    }
}
