package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Optional;

// TODO(Siri): fjern JSON-annotering når fakta-fødsel er ute (TFP-6071)
public class BekreftetBarnDto {
    // TODO(Siri): Legg til når @NotNull når fakta-fødsel er ute (TFP-6071)
    @JsonProperty("fødselsdato")
    @JsonAlias("fodselsdato")
    private LocalDate fødselsdato;

    @JsonProperty("dødsdato")
    @JsonAlias("dodsdato")
    private LocalDate dødsdato;

    BekreftetBarnDto() {
        // For Jackson
    }

    public BekreftetBarnDto(LocalDate fødselsdato, LocalDate dødsdato) {
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
    }
}
