package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public abstract class DtoMedBehandlingId {

    @NotNull
    @Valid
    private UUID behandlingUuid;

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }
}
