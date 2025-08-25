package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.util.List;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE)
public class SjekkManglendeFødselAksjonspunktDto extends BekreftetAksjonspunktDto {

    @Valid
    private List<DokumentertBarnDto> barn;

    SjekkManglendeFødselAksjonspunktDto() {
        //For Jackson
    }

    public SjekkManglendeFødselAksjonspunktDto(String begrunnelse, List<DokumentertBarnDto> barn) {
        super(begrunnelse);
        this.barn = barn;
    }

    public List<DokumentertBarnDto> getBarn() {
        return barn == null ? List.of() : barn;
    }

}
