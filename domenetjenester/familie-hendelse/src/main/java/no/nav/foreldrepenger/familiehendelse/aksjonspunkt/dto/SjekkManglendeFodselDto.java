package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE)
public class SjekkManglendeFodselDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean dokumentasjonForeligger;

    @Valid
    @JsonProperty("dokumenterteBarn")
    @JsonAlias("uidentifiserteBarn")
    private List<BekreftetBarnDto> dokumenterteBarn;

    SjekkManglendeFodselDto() {
        //For Jackson
    }

    public SjekkManglendeFodselDto(String begrunnelse, Boolean dokumentasjonForeligger, List<BekreftetBarnDto> dokumenterteBarn) {
        super(begrunnelse);
        this.dokumentasjonForeligger = dokumentasjonForeligger;

        this.dokumenterteBarn = dokumenterteBarn;
    }

    public Boolean getDokumentasjonForeligger() {
        return dokumentasjonForeligger;
    }

    public List<BekreftetBarnDto> getDokumenterteBarn() {
        return dokumenterteBarn;
    }

}
