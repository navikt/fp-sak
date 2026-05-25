package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

import java.time.LocalDate;

/** Prefilled address validity period (addressValidity component). */
public record AddressValidity(LocalDate fraOgMedDato, LocalDate tilOgMedDato) {}
