package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.person.verge.dto.VergeBehandlingsmenyEnum;

public record BehandlingOperasjonerDto(UUID uuid,
                                       @NotNull boolean behandlingKanBytteEnhet,
                                       @NotNull boolean behandlingKanHenlegges,
                                       @NotNull boolean behandlingKanGjenopptas,
                                       @NotNull boolean behandlingKanOpnesForEndringer,
                                       @NotNull boolean behandlingKanMerkesHaster,
                                       @NotNull boolean behandlingKanSettesPaVent,
                                       @NotNull boolean behandlingKanSendeMelding,
                                       @NotNull boolean behandlingFraBeslutter,
                                       @NotNull boolean behandlingTilGodkjenning,
                                       @NotNull VergeBehandlingsmenyEnum vergeBehandlingsmeny) {


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
