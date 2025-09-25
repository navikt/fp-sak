package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record DokumentertBarnDto(@NotNull LocalDate fødselsdato, LocalDate dødsdato) {
}
