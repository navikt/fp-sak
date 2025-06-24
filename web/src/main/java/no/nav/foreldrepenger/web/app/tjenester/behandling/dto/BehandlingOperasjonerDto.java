package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.UUID;

import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;

public record BehandlingOperasjonerDto(UUID uuid,
                                       boolean behandlingKanBytteEnhet,
                                       boolean behandlingKanHenlegges,
                                       boolean behandlingKanGjenopptas,
                                       boolean behandlingKanOpnesForEndringer,
                                       boolean behandlingKanMerkesHaster,
                                       boolean behandlingKanSettesPaVent,
                                       boolean behandlingKanSendeMelding,
                                       boolean behandlingFraBeslutter,
                                       boolean behandlingTilGodkjenning,
                                       VergeBehandlingsmenyEnum vergeBehandlingsmeny) {


    public BehandlingOperasjonerDto(UUID uuid) {
        this(uuid, false, false, false, false,
            false, false, false, false,
            false, VergeBehandlingsmenyEnum.SKJUL);
    }

    public BehandlingOperasjonerDto(UUID uuid, boolean behandlingTilGodkjenning) {
        this(uuid, false, false, false, false,
            false, false, false, false,
            behandlingTilGodkjenning, VergeBehandlingsmenyEnum.SKJUL);
    }

}
