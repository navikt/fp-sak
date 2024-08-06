package no.nav.foreldrepenger.tilganger;

import java.util.Collection;

record LdapBruker(String displayName, String fornavnEtternavn, Collection<String> groups) {
}
