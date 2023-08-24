package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingStegDvh;

import java.time.LocalDateTime;
import java.util.Optional;

class BehandlingStegDvhMapper {

    private BehandlingStegDvhMapper() {
    }

    static BehandlingStegDvh map(BehandlingStegTilstandSnapshot behandlingStegTilstand, Long behandlingId) {
        return BehandlingStegDvh.builder()
            .behandlingId(behandlingId)
            .behandlingStegId(behandlingStegTilstand.getId())
            .behandlingStegStatus(finnBehandlingStegStatusKode(behandlingStegTilstand))
            .behandlingStegType(behandlingStegTilstand.getSteg().getKode())
            .endretAv("VL")
            .funksjonellTid(LocalDateTime.now())
            .build();
    }

    private static String finnBehandlingStegStatusKode(BehandlingStegTilstandSnapshot behandlingStegTilstand) {
        return Optional.ofNullable(behandlingStegTilstand)
                .map(BehandlingStegTilstandSnapshot::getStatus)
                .map(BehandlingStegStatus::getKode).orElse(null);
    }
}
