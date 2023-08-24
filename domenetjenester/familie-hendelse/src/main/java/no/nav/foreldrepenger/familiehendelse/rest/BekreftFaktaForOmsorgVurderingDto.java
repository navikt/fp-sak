package no.nav.foreldrepenger.familiehendelse.rest;


import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
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
