package no.nav.foreldrepenger.web.app.tjenester.behandling.verge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.AbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import java.util.UUID;

import static no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType.BEHANDLING_UUID;

public class BehandlingsUuidDto implements AbacDto {
    @Valid
    @NotNull
    private UUID behandlingUuid;

    BehandlingsUuidDto() {
    }

    public BehandlingsUuidDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public BehandlingsUuidDto(String id) {
        this(UUID.fromString(id));
    }

    public BehandlingsUuidDto(UuidDto uuidDto) {
        this(uuidDto.getBehandlingUuid());
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(BEHANDLING_UUID, behandlingUuid.toString());
    }
}
