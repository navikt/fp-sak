package no.nav.foreldrepenger.mottak.vedtak.rest;

import java.time.LocalDate;

public record InfotrygdRestanseDto(String fnr, String valg, String type, LocalDate registrert, LocalDate mottatt, LocalDate vedtatt, String reellEnhet, String behandlendeEnhet) {
}
