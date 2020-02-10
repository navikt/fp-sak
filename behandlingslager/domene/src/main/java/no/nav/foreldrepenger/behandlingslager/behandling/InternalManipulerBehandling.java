package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class InternalManipulerBehandling {

    @Inject
    public InternalManipulerBehandling() {
    }

    public void forceOppdaterBehandlingSteg(Behandling behandling, BehandlingStegType stegType) {
        forceOppdaterBehandlingSteg(behandling, stegType, BehandlingStegStatus.UDEFINERT, BehandlingStegStatus.UTFØRT);
    }

    public void forceOppdaterBehandlingSteg(Behandling behandling, BehandlingStegType stegType, BehandlingStegStatus nesteStegStatus, BehandlingStegStatus ikkeFerdigStegStatus) {

        // finn riktig mapping av kodeverk slik at vi får med dette når Behandling brukes videre.
        Optional<BehandlingStegTilstand> eksisterendeTilstand = behandling.getSisteBehandlingStegTilstand();
        if (eksisterendeTilstand.isEmpty() || erUlikeSteg(stegType, eksisterendeTilstand)) {
            if (eksisterendeTilstand.isPresent() && !BehandlingStegStatus.erSluttStatus(eksisterendeTilstand.get().getBehandlingStegStatus())) {
                eksisterendeTilstand.ifPresent(it -> it.setBehandlingStegStatus(ikkeFerdigStegStatus));
            }
            BehandlingStegTilstand tilstand = new BehandlingStegTilstand(behandling, stegType);
            tilstand.setBehandlingStegStatus(nesteStegStatus);
            behandling.oppdaterBehandlingStegOgStatus(tilstand);
        } else {
            eksisterendeTilstand.ifPresent(it -> it.setBehandlingStegStatus(nesteStegStatus));
        }
    }

    private boolean erUlikeSteg(BehandlingStegType stegType, Optional<BehandlingStegTilstand> eksisterendeTilstand) {
        return !eksisterendeTilstand.get().getBehandlingSteg().equals(stegType);
    }

}
