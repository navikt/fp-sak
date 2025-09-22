package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record DokumentertBarnDto(@NotNull LocalDate fødselsdato, LocalDate dødsdato) {
}
