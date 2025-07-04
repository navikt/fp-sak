package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Optional;

public class DokumentertBarnDto {

    @NotNull
    private LocalDate fødselsdato;

    private LocalDate dødsdato;

    DokumentertBarnDto() {
        // For Jackson
    }

    public DokumentertBarnDto(LocalDate fødselsdato, LocalDate dødsdato) {
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
