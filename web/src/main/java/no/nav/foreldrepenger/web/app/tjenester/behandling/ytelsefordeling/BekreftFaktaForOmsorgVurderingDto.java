package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;


import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_LØPENDE_OMSORG)
public class BekreftFaktaForOmsorgVurderingDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean omsorg;

    BekreftFaktaForOmsorgVurderingDto() {
        //For Jackson
    }

    public BekreftFaktaForOmsorgVurderingDto(String begrunnelse) {
        super(begrunnelse);
    }

    public Boolean getOmsorg() {
        return omsorg;
    }

    public void setOmsorg(Boolean omsorg) {
        this.omsorg = omsorg;
    }

}
