package no.nav.foreldrepenger.web.app.tjenester.integrasjonstatus;

import java.time.LocalDateTime;

public record SystemNedeDto(String systemNavn, String endepunkt, LocalDateTime nedeFremTilTidspunkt, String feilmelding, String stackTrace) {
}
