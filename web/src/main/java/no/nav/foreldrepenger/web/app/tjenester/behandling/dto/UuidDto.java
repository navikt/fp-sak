package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.UUID;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UuidDto {

    public static final String NAME = "uuid";

    public static final String DESC = "behandlingUUID";

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av
     * eksterne systemer).
     */
    @Valid
    private UUID behandlingUuid;

    public UuidDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
    }

    public UuidDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    @JsonProperty(NAME)
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

}
