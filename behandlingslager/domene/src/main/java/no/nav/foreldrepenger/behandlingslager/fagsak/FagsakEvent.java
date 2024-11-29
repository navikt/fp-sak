package no.nav.foreldrepenger.behandlingslager.fagsak;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Marker interface for events fyrt på en Fagsak.
 * Disse fyres ved hjelp av CDI Events.
 */
public interface FagsakEvent {

    Long getFagsakId();

    Saksnummer getSaksnummer();

}
