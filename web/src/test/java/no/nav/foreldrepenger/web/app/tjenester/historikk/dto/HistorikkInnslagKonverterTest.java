package no.nav.foreldrepenger.web.app.tjenester.historikk.dto;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDto;

public class HistorikkInnslagKonverterTest {

    @Test
    public void skalSetteDokumentLinksSomUtgåttHvisTomListeAvArkivJournalPost() {
        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = HistorikkInnslagKonverter.mapFra(historikkinnslag, Collections.emptyList());
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomUtgåttHvisIkkeFinnesMatchendeArkivJournalPost() {
        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = HistorikkInnslagKonverter.mapFra(historikkinnslag, Collections.singletonList(new JournalpostId(2L)));
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomIkkeUtgåttHvisFinnesMatchendeArkivJournalPost() {

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = HistorikkInnslagKonverter.mapFra(historikkinnslag, Collections.singletonList(journalpostId));
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isFalse();
    }
}
