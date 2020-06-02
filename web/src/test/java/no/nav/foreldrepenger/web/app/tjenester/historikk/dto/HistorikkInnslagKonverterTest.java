package no.nav.foreldrepenger.web.app.tjenester.historikk.dto;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.foreldrepenger.historikk.dto.HistorikkinnslagDto;

public class HistorikkInnslagKonverterTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Test
    public void skalSetteDokumentLinksSomUtgåttHvisTomListeAvArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = konverterer.mapFra(historikkinnslag, Collections.emptyList());
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomUtgåttHvisIkkeFinnesMatchendeArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = konverterer.mapFra(historikkinnslag, Collections.singletonList(new JournalpostId(2L)));
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isTrue();
    }

    @Test
    public void skalSetteDokumentLinksSomIkkeUtgåttHvisFinnesMatchendeArkivJournalPost() {
        HistorikkInnslagKonverter konverterer = konverterer();

        HistorikkinnslagDokumentLink lenke = new HistorikkinnslagDokumentLink();
        JournalpostId journalpostId = new JournalpostId(1L);
        lenke.setJournalpostId(journalpostId);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setDokumentLinker(Collections.singletonList(lenke));
        HistorikkinnslagDto resultat = konverterer.mapFra(historikkinnslag, Collections.singletonList(journalpostId));
        assertThat(resultat.getDokumentLinks().get(0).isUtgått()).isFalse();
    }

    private HistorikkInnslagKonverter konverterer() {
        return new HistorikkInnslagKonverter();
    }
}
