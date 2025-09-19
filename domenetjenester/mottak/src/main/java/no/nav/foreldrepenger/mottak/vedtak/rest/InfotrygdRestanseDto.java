package no.nav.foreldrepenger.mottak.vedtak.rest;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InfotrygdRestanseDto(
    @NotNull String fnr,
    @NotNull String valg,
    @NotNull String type,
    @NotNull LocalDate registrert,
    @NotNull LocalDate mottatt,
    @NotNull LocalDate vedtatt,
    @NotNull String reellEnhet,
    @NotNull String behandlendeEnhet) {
}
