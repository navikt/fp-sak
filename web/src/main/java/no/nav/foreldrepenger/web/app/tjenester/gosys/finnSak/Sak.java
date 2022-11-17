package no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak;

import java.time.LocalDateTime;

public record Sak(String sakId, LocalDateTime opprettet, Saksstatus status, LocalDateTime endret, Behandlingstema behandlingstema) {
}

