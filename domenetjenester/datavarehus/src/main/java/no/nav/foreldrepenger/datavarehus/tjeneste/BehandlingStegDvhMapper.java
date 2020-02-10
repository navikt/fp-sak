package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingStegDvh;
import no.nav.vedtak.util.FPDateUtil;

public class BehandlingStegDvhMapper {

    public BehandlingStegDvh map(BehandlingStegTilstandSnapshot behandlingStegTilstand, Long behandlingId) {
        return BehandlingStegDvh.builder()
            .behandlingId(behandlingId)
            .behandlingStegId(behandlingStegTilstand.getId())
            .behandlingStegStatus(finnBehandlingStegStatusKode(behandlingStegTilstand))
            .behandlingStegType(behandlingStegTilstand.getSteg().getKode())
            .endretAv("VL")
            .funksjonellTid(FPDateUtil.nå())
            .build();
    }

    private String finnBehandlingStegStatusKode(BehandlingStegTilstandSnapshot behandlingStegTilstand) {
        return Optional.ofNullable(behandlingStegTilstand)
                .map(BehandlingStegTilstandSnapshot::getStatus)
                .map(BehandlingStegStatus::getKode).orElse(null);
    }
}
