package no.nav.foreldrepenger.web.app.tjenester.historikk.dto;


import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;

class HistorikkInnslagKonverterTest {

    private static final URI DUMMY = URI.create("http://dummy/dummy");

    @Test
    void skalSetteDokumentLinksSomUtgåttHvisTomListeAvArkivJournalPost() {
        var lenke = new HistorikkinnslagDokumentLink();
        var journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        var resultat = HistorikkInnslagKonverter.mapFra(historikkinnslag, Collections.emptyList(), null, DUMMY);
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    void skalSetteDokumentLinksSomUtgåttHvisIkkeFinnesMatchendeArkivJournalPost() {
        var lenke = new HistorikkinnslagDokumentLink();
        var journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        var resultat = HistorikkInnslagKonverter.mapFra(historikkinnslag, Collections.singletonList(new JournalpostId(2L)), null, DUMMY);
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    void skalSetteDokumentLinksSomIkkeUtgåttHvisFinnesMatchendeArkivJournalPost() {

        var lenke = new HistorikkinnslagDokumentLink();
        var journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        var resultat = HistorikkInnslagKonverter.mapFra(historikkinnslag, Collections.singletonList(journalpostId), null, DUMMY);
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isFalse();
    }
}
