package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.adopsjon.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE_KODE)
public class BekreftMannAdoptererAksjonspunktDto extends BekreftetAksjonspunktDto {


    @NotNull
    private Boolean mannAdoptererAlene;

    BekreftMannAdoptererAksjonspunktDto() {
        //For Jackson
    }

    public BekreftMannAdoptererAksjonspunktDto(String begrunnelse, Boolean mannAdoptererAlene) {
        super(begrunnelse);
        this.mannAdoptererAlene = mannAdoptererAlene;
    }


    public Boolean getMannAdoptererAlene() {
        return mannAdoptererAlene;
    }

}
