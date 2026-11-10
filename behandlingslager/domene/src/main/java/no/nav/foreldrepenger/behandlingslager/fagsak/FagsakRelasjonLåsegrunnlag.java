package no.nav.foreldrepenger.behandlingslager.fagsak;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Data som skal låses før tilgang til FagsakRelasjon. Låsen må tas før man henter FagsakRelasjon for å unngå race-conditions.
 */
public record FagsakRelasjonLåsegrunnlag(Long relasjonId, Saksnummer saksnummerEn, Saksnummer saksnummerTo) {

}
