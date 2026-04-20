package no.nav.foreldrepenger.mottak.fyllutsendinn;

import jakarta.validation.constraints.NotNull;

/** Shared record for country-picker (landvelger) fields with country code and label. */
public record Landvalg(@NotNull String value, @NotNull String label) {}
