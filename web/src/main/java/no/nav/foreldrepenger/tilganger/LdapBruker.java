package no.nav.foreldrepenger.tilganger;

import java.util.Collection;

record LdapBruker(String displayName, Collection<String> groups) {
}
