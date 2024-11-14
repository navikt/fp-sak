package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;

class HistorikkinnslagTjenesteTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("5");
    private static final String HOVEDDOKUMENT_DOKUMENT_ID = "1";
    private static final String VEDLEGG_DOKUMENT_ID = "2";

    private HistorikkRepository historikkRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @BeforeEach
    public void before() {
        historikkRepository = mock(HistorikkRepository.class);
        dokumentArkivTjeneste = mock(DokumentArkivTjeneste.class);
        historikkinnslagTjeneste = new HistorikkinnslagTjeneste(historikkRepository, mock(Historikkinnslag2Repository.class), dokumentArkivTjeneste);
    }

    @Test
    void skal_lagre_historikkinnslag_for_elektronisk_søknad() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var behandling = scenario.lagMocked();
        // Arrange

        when(dokumentArkivTjeneste.hentJournalpostForSak(eq(JOURNALPOST_ID)))
            .thenReturn(Optional.of(byggJournalpost(JOURNALPOST_ID, HOVEDDOKUMENT_DOKUMENT_ID, Collections.singletonList(VEDLEGG_DOKUMENT_ID))));

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, JOURNALPOST_ID, false, true, false);

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SØKER);
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.BEH_STARTET);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).isNotEmpty();

        var dokumentLinker = historikkinnslag.getDokumentLinker();
        assertThat(dokumentLinker).hasSize(2);
        assertThat(dokumentLinker.get(0).getDokumentId()).isEqualTo(HOVEDDOKUMENT_DOKUMENT_ID);
        assertThat(dokumentLinker.get(0).getJournalpostId()).isEqualTo(JOURNALPOST_ID);
        assertThat(dokumentLinker.get(0).getLinkTekst()).isEqualTo("Søknad");
        assertThat(dokumentLinker.get(1).getDokumentId()).isEqualTo(VEDLEGG_DOKUMENT_ID);
        assertThat(dokumentLinker.get(1).getJournalpostId()).isEqualTo(JOURNALPOST_ID);
        assertThat(dokumentLinker.get(1).getLinkTekst()).isEqualTo("Vedlegg");
    }

    @Test
    void skal_lagre_historikkinnslag_for_papir_søknad() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var behandling = scenario.lagMocked();
        // Arrange

        when(dokumentArkivTjeneste.hentJournalpostForSak(eq(JOURNALPOST_ID)))
            .thenReturn(Optional.of(byggJournalpost(JOURNALPOST_ID, HOVEDDOKUMENT_DOKUMENT_ID, Collections.emptyList())));

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, JOURNALPOST_ID, false, false, false);

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        var dokumentLinker = historikkinnslag.getDokumentLinker();
        assertThat(dokumentLinker).hasSize(1);
        assertThat(dokumentLinker.get(0).getDokumentId()).isEqualTo(HOVEDDOKUMENT_DOKUMENT_ID);
        assertThat(dokumentLinker.get(0).getJournalpostId()).isEqualTo(JOURNALPOST_ID);
        assertThat(dokumentLinker.get(0).getLinkTekst()).isEqualTo("Papirsøknad");
    }

    @Test
    void skal_lagre_historikkinnslag_for_im()  {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var behandling = scenario.lagMocked();
        // Arrange

        when(dokumentArkivTjeneste.hentJournalpostForSak(eq(JOURNALPOST_ID)))
            .thenReturn(Optional.of(byggJournalpost(JOURNALPOST_ID, HOVEDDOKUMENT_DOKUMENT_ID, Collections.emptyList())));

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsak(), JOURNALPOST_ID, DokumentTypeId.INNTEKTSMELDING, true);

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        var dokumentLinker = historikkinnslag.getDokumentLinker();
        assertThat(dokumentLinker).hasSize(1);
        assertThat(dokumentLinker.get(0).getDokumentId()).isEqualTo(HOVEDDOKUMENT_DOKUMENT_ID);
        assertThat(dokumentLinker.get(0).getJournalpostId()).isEqualTo(JOURNALPOST_ID);
        assertThat(dokumentLinker.get(0).getLinkTekst()).isEqualTo("Inntektsmelding");
    }

    @Test
    void skal_lagre_historikkinnslag_for_vedlegg() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        var behandling = scenario.lagMocked();
        // Arrange

        when(dokumentArkivTjeneste.hentJournalpostForSak(eq(JOURNALPOST_ID)))
            .thenReturn(Optional.of(byggJournalpost(JOURNALPOST_ID, HOVEDDOKUMENT_DOKUMENT_ID, Collections.emptyList())));

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsak(), JOURNALPOST_ID, DokumentTypeId.ANNET, false);

        // Assert
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        var dokumentLinker = historikkinnslag.getDokumentLinker();
        assertThat(dokumentLinker).hasSize(1);
        assertThat(dokumentLinker.get(0).getDokumentId()).isEqualTo(HOVEDDOKUMENT_DOKUMENT_ID);
        assertThat(dokumentLinker.get(0).getJournalpostId()).isEqualTo(JOURNALPOST_ID);
        assertThat(dokumentLinker.get(0).getLinkTekst()).isEqualTo("Ettersendelse");
    }

    @Test
    void skal_ikke_lagre_historikkinnslag_når_det_allerede_finnes()  {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagMocked();

        var eksisterendeHistorikkinnslag = new Historikkinnslag();
        eksisterendeHistorikkinnslag.setType(HistorikkinnslagType.BEH_STARTET);
        when(historikkRepository.hentHistorikk(behandling.getId())).thenReturn(Collections.singletonList(eksisterendeHistorikkinnslag));

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, JOURNALPOST_ID, false, true, false);

        // Assert
        verify(historikkRepository, times(0)).lagre(any(Historikkinnslag.class));
    }

    @Test
    void skal_støtte_at_journalpostId_er_null_og_ikke_kalle_journalTjeneste()  {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagMocked();

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, null, false, false, false);

        // Assert
        verify(dokumentArkivTjeneste, times(0)).hentJournalpostForSak(any(JournalpostId.class));
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getDokumentLinker()).isEmpty();
    }

    private ArkivJournalPost byggJournalpost(JournalpostId journalpostId, String dokumentId, List<String> vedleggDokID) {
        var builder = ArkivJournalPost.Builder.ny()
            .medJournalpostId(journalpostId)
            .medHoveddokument(ArkivDokument.Builder.ny().medDokumentId(dokumentId).build());
        vedleggDokID.forEach(vid -> builder.leggTillVedlegg(ArkivDokument.Builder.ny().medDokumentId(vid).build()));
        return builder.build();
    }

}
