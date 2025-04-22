package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public record BehandlingStegTilstandSnapshot(BehandlingStegType steg, BehandlingStegStatus status) {


    public static BehandlingStegTilstandSnapshot tilBehandlingsStegSnapshot(Behandling behandling) {
        var tilstand = behandling.getBehandlingStegTilstand();
        var stegType = tilstand.map(BehandlingStegTilstand::getBehandlingSteg);
        var status = tilstand.map(BehandlingStegTilstand::getBehandlingStegStatus).orElse(null);
        return stegType.map(s -> new BehandlingStegTilstandSnapshot(s, status)).orElse(null);
    }

    public static BehandlingStegTilstandSnapshot tilBehandlingsStegSnapshot(Behandling behandling, BehandlingStegType steg) {
        var tilstand = behandling.getBehandlingStegTilstand(steg);
        var stegType = tilstand.map(BehandlingStegTilstand::getBehandlingSteg);
        var status = tilstand.map(BehandlingStegTilstand::getBehandlingStegStatus).orElse(null);
        return stegType.map(s -> new BehandlingStegTilstandSnapshot(s, status)).orElse(null);
    }

    public static BehandlingStegTilstandSnapshot tilBehandlingsStegSnapshotSiste(Behandling behandling) {
        var tilstand = behandling.getSisteBehandlingStegTilstand();
        var stegType = tilstand.map(BehandlingStegTilstand::getBehandlingSteg);
        var status = tilstand.map(BehandlingStegTilstand::getBehandlingStegStatus).orElse(null);
        return stegType.map(s -> new BehandlingStegTilstandSnapshot(s, status)).orElse(null);
    }

}
