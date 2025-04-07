package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.RettighetType;

@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_RETT_OG_OMSORG)
public class OverstyrOmsorgOgRettDto extends BekreftetAksjonspunktDto {

    @NotNull
    private RettighetType rettighetType;

    OverstyrOmsorgOgRettDto() {
        // For Jackson
    }

    public OverstyrOmsorgOgRettDto(String begrunnelse) {
        super(begrunnelse);
    }

    public RettighetType getRettighetType() {
        return rettighetType;
    }

    public void setRettighetType(RettighetType rettighetType) {
        this.rettighetType = rettighetType;
    }
}
