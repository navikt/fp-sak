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
