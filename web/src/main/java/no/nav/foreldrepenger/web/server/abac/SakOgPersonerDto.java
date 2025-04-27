package no.nav.foreldrepenger.web.server.abac;

import java.util.Set;

public record SakOgPersonerDto(String saksnummer, String saksident, Set<String> identer) {
}
