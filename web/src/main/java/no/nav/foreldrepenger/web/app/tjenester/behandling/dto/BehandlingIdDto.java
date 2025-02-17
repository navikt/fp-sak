package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class BehandlingIdDto  {

    public static final String NAME = "behandlingUuid";

    @Valid
    @NotNull
    private UUID behandlingUuid;

    BehandlingIdDto() {

    }

    public BehandlingIdDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public BehandlingIdDto(String id) {
        this(UUID.fromString(id));
    }

    public BehandlingIdDto(UuidDto uuidDto) {
        this(uuidDto.getBehandlingUuid());
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public String toString() {
        return behandlingUuid.toString();
    }
}
