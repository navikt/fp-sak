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


    public static BehandlingOperasjonerDto avsluttet(UUID uuid) {
        return new BehandlingOperasjonerDto(uuid, false, false, false, false,
            false, false, false, false,
            false, VergeBehandlingsmenyEnum.SKJUL);
    }

    public static BehandlingOperasjonerDto fatteVedtak(UUID uuid, boolean behandlingTilGodkjenning, boolean kanMerkesHaster) {
        return new BehandlingOperasjonerDto(uuid, false, false, false, false,
            kanMerkesHaster, false, false, false,
            behandlingTilGodkjenning, VergeBehandlingsmenyEnum.SKJUL);
    }

    public static BehandlingOperasjonerDto veileder(UUID uuid, boolean kanMerkesHaster) {
        return new BehandlingOperasjonerDto(uuid, false, false, false, false,
            kanMerkesHaster, false, false, false,
            false, VergeBehandlingsmenyEnum.SKJUL);
    }

}
