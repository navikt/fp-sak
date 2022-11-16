package no.nav.foreldrepenger.web.app.tjenester.gosys;

import java.time.LocalDateTime;

public record Sak(String sakId, LocalDateTime opprettet, Saksstatus status, LocalDateTime endret, Behandlingstema behandlingstema) {
}

record Saksstatus(String termnavn, String value, String kodeverkRef, String kodeRef) {
}

record Behandlingstema(String termnavn, String value, String kodeverkRef, String kodeRef) {
}
