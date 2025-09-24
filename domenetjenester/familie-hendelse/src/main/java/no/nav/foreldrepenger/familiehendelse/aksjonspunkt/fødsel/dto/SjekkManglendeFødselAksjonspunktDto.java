package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE)
public class SjekkManglendeFødselAksjonspunktDto extends BekreftetAksjonspunktDto {

    // barn = null betyr at barn ikke er født
    @Valid
    @Size(max = 9)
    private List<DokumentertBarnDto> barn;

    private LocalDate termindato;

    SjekkManglendeFødselAksjonspunktDto() {
        //For Jackson
    }

    public SjekkManglendeFødselAksjonspunktDto(String begrunnelse, List<DokumentertBarnDto> barn) {
        super(begrunnelse);
        this.barn = barn;
    }

    public Optional<List<DokumentertBarnDto>> getBarn() {
        return Optional.ofNullable(barn);
    }

    public Optional<LocalDate> getTermindato() {
        return Optional.ofNullable(termindato);
    }
}
