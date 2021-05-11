package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public abstract class DtoMedBehandlingId {
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @Valid
    private UUID behandlingUuid;

    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }
}
