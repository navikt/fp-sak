package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType.INNHENTE_OPPLYSNINGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType.KLAGE_OVERSENDT;
import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType.VARSEL_OM_REVURDERING;
import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType.VARSEL_REVURDERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentarkiv.DokumentRespons;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentForhandsvisning;
import no.nav.foreldrepenger.dokumentbestiller.DokumentForhåndsvisningTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillDokumentDto;
import no.nav.foreldrepenger.dokumentbestiller.dto.ForhåndsvisDokumentDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;
    private DokumentForhåndsvisningTjeneste dokumentForhåndsvisningTjenesteMock;
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private MellomlagringRepository mellomlagringRepositoryMock;
    private DokumentArkivTjeneste dokumentArkivTjenesteMock;

    @BeforeEach
    void setUp() {
        dokumentBestillerTjenesteMock = mock(DokumentBestillerTjeneste.class);
        dokumentForhåndsvisningTjenesteMock = mock(DokumentForhåndsvisningTjeneste.class);
        dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        arbeidsforholdInntektsmeldingMangelTjeneste = mock(ArbeidsforholdInntektsmeldingMangelTjeneste.class);
        mellomlagringRepositoryMock = mock(MellomlagringRepository.class);
        dokumentArkivTjenesteMock = mock(DokumentArkivTjeneste.class);

        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(dokumentForhåndsvisningTjenesteMock, dokumentBestillerTjenesteMock,
            dokumentBehandlingTjenesteMock, behandlingRepository, arbeidsforholdInntektsmeldingMangelTjeneste,
            mock(HistorikkinnslagRepository.class), mellomlagringRepositoryMock, dokumentArkivTjenesteMock);
    }

    @Test
    void bestillerDokument() {
        // Arrange
        var behandlingUuid = UUID.randomUUID();
        var behandlingMock = mock(Behandling.class);
        when(behandlingMock.getSaksnummer()).thenReturn(new Saksnummer("9999"));
        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(behandlingMock);
        var dokumentMal = INNHENTE_OPPLYSNINGER;
        var bestillBrevDto = new BestillDokumentDto(behandlingUuid, dokumentMal, null);

        // Act
        brevRestTjeneste.bestillDokument(bestillBrevDto);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentBestilling.class);

        // Assert
        verify(dokumentBestillerTjenesteMock).bestillDokument(bestillingCaptor.capture());
        verify(behandlingRepository).hentBehandling(behandlingUuid);

        var bestilling = bestillingCaptor.getValue();

        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.behandlingUuid()).isEqualTo(behandlingUuid);
        assertThat(bestilling.dokumentMal()).isEqualTo(dokumentMal);
        assertThat(bestilling.fritekst()).isNull();
        assertThat(bestilling.revurderingÅrsak()).isNull();
        assertThat(bestilling.journalførSom()).isNull();
    }

    @Test
    void hent_brev_html_returnerer_opprinnelig_og_mellomlagret() {
        var brevProdusertAvFpdokgen = "html";
        var mellomlagretHtml = "mellomlagret varsel";
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.genererHtml(behandling, VARSEL_OM_REVURDERING, null)).thenReturn(brevProdusertAvFpdokgen);
        var mellomlagringEntitet = MellomlagringEntitet.Builder.ny()
            .medBehandlingId(behandling.getId())
            .medType(VARSEL_REVURDERING)
            .medInnhold(mellomlagretHtml)
            .build();
        when(mellomlagringRepositoryMock.hentMellomlagring(behandling.getId(), VARSEL_REVURDERING))
            .thenReturn(Optional.of(mellomlagringEntitet));

        // Act
        var respons = brevRestTjeneste.hentBrevHtml(
            new BrevRestTjeneste.BrevHtmlDto(UUID.randomUUID(), VARSEL_OM_REVURDERING, null));

        // Assert
        var dto = (BrevRestTjeneste.OverstyrtDokumentDto) respons.getEntity();
        assertThat(dto.opprinneligHtml()).isEqualTo(brevProdusertAvFpdokgen);
        assertThat(dto.redigertHtml()).isEqualTo(mellomlagretHtml);
    }

    @Test
    void hent_brev_html_uten_mellomlagret_innhold_gir_null_redigert() {
        var brevProdusertAvFpdokgen = "html";
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.genererHtml(behandling, VARSEL_OM_REVURDERING, null)).thenReturn(brevProdusertAvFpdokgen);
        when(mellomlagringRepositoryMock.hentMellomlagring(behandling.getId(), VARSEL_REVURDERING))
            .thenReturn(Optional.empty());

        // Act
        var respons = brevRestTjeneste.hentBrevHtml(
            new BrevRestTjeneste.BrevHtmlDto(UUID.randomUUID(), VARSEL_OM_REVURDERING, null));

        // Assert
        var dto = (BrevRestTjeneste.OverstyrtDokumentDto) respons.getEntity();
        assertThat(dto.opprinneligHtml()).isEqualTo(brevProdusertAvFpdokgen);
        assertThat(dto.redigertHtml()).isNull();
    }

    @Test
    void hent_brev_html_for_vedtaksbrev_bruker_gammel_overstyring() {
        var brevProdusertAvFpdokgen = "vedtaksbrev html";
        var overstyrtHtml = "redigert vedtaksbrev";
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.genererHtml(behandling)).thenReturn(brevProdusertAvFpdokgen);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.of(overstyrtHtml));

        // Act
        var respons = brevRestTjeneste.hentBrevHtml(
            new BrevRestTjeneste.BrevHtmlDto(UUID.randomUUID(), null, null));
    }

    @Test
    void hent_brev_html_for_vedtaksbrev_uten_overstyring() {
        var brevProdusertAvFpdokgen = "vedtaksbrev html";
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.genererHtml(behandling)).thenReturn(brevProdusertAvFpdokgen);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.empty());

        // Act
        var respons = brevRestTjeneste.hentBrevHtml(
            new BrevRestTjeneste.BrevHtmlDto(UUID.randomUUID(), null, null));

        // Assert
        var dto = (BrevRestTjeneste.OverstyrtDokumentDto) respons.getEntity();
        assertThat(dto.opprinneligHtml()).isEqualTo(brevProdusertAvFpdokgen);
        assertThat(dto.redigertHtml()).isNull();
    }

    @Test
    void forhåndsvis_klage_oversendt_med_fritekst_bruker_ikke_fritekst_html_som_dokumentmal() {
        var behandlingUuid = UUID.randomUUID();
        var behandling = mock(Behandling.class);
        var pdf = "pdf".getBytes();
        when(behandling.getSaksnummer()).thenReturn(new Saksnummer("9999"));
        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.forhåndsvisDokument(any())).thenReturn(pdf);
        var dto = new ForhåndsvisDokumentDto(behandlingUuid, KLAGE_OVERSENDT, null, false, "fritekst");

        var respons = brevRestTjeneste.forhåndsvisDokument(dto);

        var captor = ArgumentCaptor.forClass(DokumentForhandsvisning.class);
        verify(dokumentForhåndsvisningTjenesteMock).forhåndsvisDokument(captor.capture());
        assertThat(captor.getValue().dokumentMal()).isEqualTo(KLAGE_OVERSENDT);
        assertThat(captor.getValue().fritekst()).isEqualTo("fritekst");
        assertThat(respons.getStatus()).isEqualTo(200);
    }

    @Test
    void hent_overstyrt_vedtaksbrev_returnerer_pdf_fra_mellomlagring() {
        var behandlingUuid = UUID.randomUUID();
        var pdf = "pdf".getBytes();
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(1L);
        when(behandling.getUuid()).thenReturn(behandlingUuid);
        when(behandling.getSaksnummer()).thenReturn(new Saksnummer("9999"));
        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(behandling);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.of("<html>redigert</html>"));
        when(dokumentForhåndsvisningTjenesteMock.forhåndsvisDokument(any())).thenReturn(pdf);

        var respons = brevRestTjeneste.hentOverstyrtVedtaksbrev(new UuidDto(behandlingUuid));

        assertThat(respons.getStatus()).isEqualTo(200);
        assertThat(respons.getEntity()).isEqualTo(pdf);
        assertThat(respons.getMediaType().toString()).isEqualTo("application/pdf");
    }

    @Test
    void hent_overstyrt_vedtaksbrev_returnerer_pdf_fra_arkiv_når_mellomlagring_mangler() {
        var behandlingUuid = UUID.randomUUID();
        var behandling = mock(Behandling.class);
        var journalpostId = new JournalpostId("123456789");
        var dokumentId = "dok1";
        var pdf = "arkiv-pdf".getBytes();
        var hovedDokument = ArkivDokument.Builder.ny().medDokumentId(dokumentId).build();
        var journalpost = ArkivJournalPost.Builder.ny().medJournalpostId(journalpostId).medHoveddokument(hovedDokument).build();
        var dokumentRespons = new DokumentRespons(pdf, "application/pdf", "filename=vedtaksbrev.pdf");
        when(behandling.getId()).thenReturn(1L);
        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(behandling);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.empty());
        when(dokumentBehandlingTjenesteMock.finnJournalpostIdForRedigertVedtaksbrev(behandling.getId())).thenReturn(Optional.of(journalpostId));
        when(dokumentArkivTjenesteMock.hentJournalpostForSak(journalpostId)).thenReturn(Optional.of(journalpost));
        when(dokumentArkivTjenesteMock.hentDokument(journalpostId, dokumentId)).thenReturn(dokumentRespons);

        var respons = brevRestTjeneste.hentOverstyrtVedtaksbrev(new UuidDto(behandlingUuid));

        assertThat(respons.getStatus()).isEqualTo(200);
        assertThat(respons.getEntity()).isEqualTo(pdf);
        assertThat(respons.getMediaType().toString()).isEqualTo("application/pdf");
    }

    @Test
    void hent_overstyrt_vedtaksbrev_returnerer_404_når_verken_mellomlagring_eller_journalpost_finnes() {
        var behandlingUuid = UUID.randomUUID();
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(1L);
        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(behandling);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.empty());
        when(dokumentBehandlingTjenesteMock.finnJournalpostIdForRedigertVedtaksbrev(behandling.getId())).thenReturn(Optional.empty());

        var respons = brevRestTjeneste.hentOverstyrtVedtaksbrev(new UuidDto(behandlingUuid));

        assertThat(respons.getStatus()).isEqualTo(404);
    }
}
