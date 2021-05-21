package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BehandlingMedUttaksperioderDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    @Valid
    private UUID behandlingUuid;

    @Valid
    @NotNull
    @Size(min = 1, max = 1500)
    private List<UttakResultatPeriodeLagreDto> perioder;

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public List<UttakResultatPeriodeLagreDto> getPerioder() {
        return perioder;
    }

    public void setPerioder(List<UttakResultatPeriodeLagreDto> perioder) {
        this.perioder = perioder;
    }
}
