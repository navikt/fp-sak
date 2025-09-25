package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.adopsjon.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN_KODE)
public class BekreftEktefelleAksjonspunktDto extends BekreftetAksjonspunktDto {


    @NotNull
    private Boolean ektefellesBarn;

    BekreftEktefelleAksjonspunktDto() {
        //For Jackson
    }

    public BekreftEktefelleAksjonspunktDto(String begrunnelse, Boolean ektefellesBarn) {
        super(begrunnelse);
        this.ektefellesBarn = ektefellesBarn;
    }


    public Boolean getEktefellesBarn() {
        return ektefellesBarn;
    }

}
