package no.nav.foreldrepenger.web.app.tjenester.behandling.verge.dto;

import io.swagger.v3.oas.annotations.Parameter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import java.util.UUID;

import static no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType.BEHANDLING_UUID;

public class BehandlingsUuidParam implements AbacDto {

    public static final String NAME = "behandlingUuid";

    @Parameter(description = "Behandlingens uuid")
    @Valid
    @NotNull
    private UUID behandlingUuid;

    public BehandlingsUuidParam(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public BehandlingsUuidParam(String uuid) {
        this(UUID.fromString(uuid));
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(BEHANDLING_UUID, behandlingUuid.toString());
    }
}
