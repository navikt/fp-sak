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
    @JsonProperty("erBarnFødt")
    @JsonAlias("dokumentasjonForeligger")
    private Boolean erBarnFødt;

    @Valid
    @JsonProperty("barn")
    @JsonAlias("uidentifiserteBarn")
    private List<BekreftetBarnDto> barn;

    SjekkManglendeFodselDto() {
        //For Jackson
    }

    public SjekkManglendeFodselDto(String begrunnelse, Boolean dokumentasjonForeligger, List<BekreftetBarnDto> barn) {
        super(begrunnelse);
        this.erBarnFødt = dokumentasjonForeligger;
        this.barn = barn;
    }

    public Boolean getErBarnFødt() {
        return erBarnFødt;
    }

    public List<BekreftetBarnDto> getBarn() {
        return barn;
    }

}
