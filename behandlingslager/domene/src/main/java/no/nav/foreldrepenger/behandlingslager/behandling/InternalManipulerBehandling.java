package no.nav.foreldrepenger.behandlingslager.behandling;

public final class InternalManipulerBehandling {

    private InternalManipulerBehandling() {
    }

    public static void forceOppdaterBehandlingSteg(Behandling behandling, BehandlingStegType stegType) {
        forceOppdaterBehandlingSteg(behandling, stegType, BehandlingStegStatus.UDEFINERT, BehandlingStegStatus.UTFØRT);
    }

    public static void forceOppdaterBehandlingSteg(Behandling behandling,
                                                   BehandlingStegType stegType,
                                                   BehandlingStegStatus nesteStegStatus,
                                                   BehandlingStegStatus ikkeFerdigStegStatus) {

        var eksisterendeTilstand = behandling.getSisteBehandlingStegTilstand();
        // Dersom eksisterende tom eller ulik neste - ny tilstand
        if (eksisterendeTilstand.filter(e -> e.getBehandlingSteg().equals(stegType)).isEmpty()) {
            if (eksisterendeTilstand.isPresent() && !BehandlingStegStatus.erSluttStatus(eksisterendeTilstand.get().getBehandlingStegStatus())) {
                eksisterendeTilstand.ifPresent(it -> it.setBehandlingStegStatus(ikkeFerdigStegStatus));
            }
            var tilstand = new BehandlingStegTilstand(behandling, stegType);
            tilstand.setBehandlingStegStatus(nesteStegStatus);
            behandling.oppdaterBehandlingStegOgStatus(tilstand);
        } else {
            eksisterendeTilstand.ifPresent(it -> it.setBehandlingStegStatus(nesteStegStatus));
        }
    }

}
