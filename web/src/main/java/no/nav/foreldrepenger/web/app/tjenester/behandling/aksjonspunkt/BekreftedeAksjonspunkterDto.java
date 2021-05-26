package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;

public class BekreftedeAksjonspunkterDto {

    @Valid
    private BehandlingIdDto behandlingId;

    @Valid
    private UUID behandlingUuid;

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    @Size(min = 1, max = 10)
    @Valid
    private Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer;

    public static BekreftedeAksjonspunkterDto lagDto(Long behandlingId,
                                                     UUID behandlingUuid,
                                                     Long behandlingVersjon,
                                                     Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer) {
        var dto = new BekreftedeAksjonspunkterDto();
        dto.behandlingId = new BehandlingIdDto(behandlingId);
        dto.behandlingVersjon = behandlingVersjon;
        dto.bekreftedeAksjonspunktDtoer = bekreftedeAksjonspunktDtoer;
        dto.behandlingUuid = behandlingUuid;
        return dto;
    }

    public Long getBehandlingId() {
        return behandlingId == null ? null : behandlingId.getBehandlingId();
    }

    public UUID getBehandlingUuid() {
        if (behandlingUuid != null) {
            return behandlingUuid;
        }
        return behandlingId != null ? behandlingId.getBehandlingUuid() : null;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public Collection<BekreftetAksjonspunktDto> getBekreftedeAksjonspunktDtoer() {
        return bekreftedeAksjonspunktDtoer;
    }
}
