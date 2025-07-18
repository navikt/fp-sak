package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE)
public class SjekkManglendeFødselAksjonspunktDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean erBarnFødt;

    @Valid
    private List<DokumentertBarnDto> barn;

    SjekkManglendeFødselAksjonspunktDto() {
        //For Jackson
    }

    public SjekkManglendeFødselAksjonspunktDto(String begrunnelse, Boolean dokumentasjonForeligger, List<DokumentertBarnDto> barn) {
        super(begrunnelse);
        this.erBarnFødt = dokumentasjonForeligger;
        this.barn = barn;
    }

    public Boolean getErBarnFødt() {
        return erBarnFødt;
    }

    public List<DokumentertBarnDto> getBarn() {
        return barn;
    }

}
