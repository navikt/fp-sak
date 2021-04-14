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

        // finn riktig mapping av kodeverk slik at vi får med dette når Behandling brukes videre.
        var eksisterendeTilstand = behandling.getSisteBehandlingStegTilstand();
        if (eksisterendeTilstand.isEmpty() || erUlikeSteg(stegType, eksisterendeTilstand.orElseThrow())) {
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

    private static boolean erUlikeSteg(BehandlingStegType stegType, BehandlingStegTilstand eksisterendeTilstand) {
        return !eksisterendeTilstand.getBehandlingSteg().equals(stegType);
    }

}
