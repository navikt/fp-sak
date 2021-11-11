package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

/**
 * Dokumentmottak i Fpsak. Underliggende klasser håndterer forskjellige typer dokumenter
 * med dedikerte mottakere, og det er InnhentDokumentTjeneste som bestemmer hvilken
 * mottaker som skal benyttes.
 *
 * Ved endringer i underliggende klasser skal dokumentasjonen oppdateres:
 * https://confluence.adeo.no/display/TVF/Dokumentmottak+i+Fpsak
 * Husk også å vedlikeholde scenario-referansene i koden.
 */
public interface Dokumentmottaker {

    void mottaDokument(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

    void mottaDokumentForKøetBehandling(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

    default boolean utsetterStartdato(MottattDokument mottattDokument, Fagsak fagsak) {
        return false;
    }

    default void utsettelseFraStart(MottattDokument mottattDokument, Fagsak fagsak) {
        throw new IllegalStateException("utsettelseFraStart er ikke implementert");
    }

    @SuppressWarnings("unused")
    default void opprettFraTidligereAvsluttetBehandling(Fagsak fagsak, Long behandlingId, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {}
}
