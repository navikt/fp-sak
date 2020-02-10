package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SkjæringstidspunktDto {

    @JsonProperty("dato")
    private final LocalDate dato;

    public SkjæringstidspunktDto(LocalDate dato) {
        this.dato = dato;
    }

    public LocalDate getDato() {
        return dato;
    }
}
