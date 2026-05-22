package no.nav.foreldrepenger.mottak.registrerer;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER_KODE)
public class PapirsoknadSvangerskapspengerMellomlagreDto extends PapirsoknadMedInntektArbeidYtelseMellomlagreDto {

    private List<@Valid TilretteleggingFormValues> tilrettelegging;

    public List<TilretteleggingFormValues> getTilrettelegging() {
        return tilrettelegging;
    }

    public void setTilrettelegging(List<TilretteleggingFormValues> tilrettelegging) {
        this.tilrettelegging = tilrettelegging;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TilretteleggingFormValues(LocalDate behovsdato, List<TilretteleggingDetaljer> tilrettelegginger) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record TilretteleggingDetaljer(String type, LocalDate dato, Integer stillingsprosent) { }
    }
}
