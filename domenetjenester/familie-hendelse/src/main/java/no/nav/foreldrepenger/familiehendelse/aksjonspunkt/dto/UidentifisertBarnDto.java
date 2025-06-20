package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Optional;

// TODO: fjern JSON-annotering når fakta-fødsel er ute (TFP-6071)
public class UidentifisertBarnDto {
    @NotNull
    @JsonProperty("fødselsdato")
    @JsonAlias("fodselsdato")
    private LocalDate fødselsdato;

    @JsonProperty("dødsdato")
    @JsonAlias("dodsdato")
    private LocalDate dødsdato;

    UidentifisertBarnDto() {
        // For Jackson
    }

    public UidentifisertBarnDto(LocalDate fødselsdato, LocalDate dødsdato) {
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
