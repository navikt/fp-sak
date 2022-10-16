package no.nav.foreldrepenger.tilganger;

import java.util.Collection;

public record LdapBruker(String displayName, Collection<String> groups) {

}
