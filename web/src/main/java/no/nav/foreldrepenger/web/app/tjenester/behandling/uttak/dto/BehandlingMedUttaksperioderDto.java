package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class BehandlingMedUttaksperioderDto {

    @Valid
    @NotNull
    private UUID behandlingUuid;

    @Valid
    @NotNull
    @Size(min = 1, max = 1500)
    private List<UttakResultatPeriodeLagreDto> perioder;

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
