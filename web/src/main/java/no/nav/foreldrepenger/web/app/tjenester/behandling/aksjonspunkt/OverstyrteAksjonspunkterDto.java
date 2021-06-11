package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import java.util.Collection;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;

public class OverstyrteAksjonspunkterDto {

    @Valid
    @NotNull
    private UUID behandlingUuid;

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    @Valid
    @Size(min = 1, max = 10)
    private Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunktDtoer;

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public Collection<OverstyringAksjonspunktDto> getOverstyrteAksjonspunktDtoer() {
        return overstyrteAksjonspunktDtoer;
    }

}
