package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType.INNHENTE_OPPLYSNINGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentForhåndsvisningTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillDokumentDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;
    private DokumentForhåndsvisningTjeneste dokumentForhåndsvisningTjenesteMock;
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    @BeforeEach
    void setUp() {
        dokumentBestillerTjenesteMock = mock(DokumentBestillerTjeneste.class);
        dokumentForhåndsvisningTjenesteMock = mock(DokumentForhåndsvisningTjeneste.class);
        dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        arbeidsforholdInntektsmeldingMangelTjeneste = mock(ArbeidsforholdInntektsmeldingMangelTjeneste.class);

        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(dokumentForhåndsvisningTjenesteMock, dokumentBestillerTjenesteMock,
            dokumentBehandlingTjenesteMock, behandlingRepository, arbeidsforholdInntektsmeldingMangelTjeneste, mock(HistorikkinnslagRepository.class));
    }

    @Test
    void bestillerDokument() {
        // Arrange
        var behandlingUuid = UUID.randomUUID();
        var behandlingMock = mock(Behandling.class);
        when(behandlingMock.getSaksnummer()).thenReturn(new Saksnummer("9999"));
        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(behandlingMock);
        var fritekst = "Dette er en fritekst";
        var dokumentMal = INNHENTE_OPPLYSNINGER;
        var bestillBrevDto = new BestillDokumentDto(behandlingUuid, dokumentMal, fritekst, null, null);

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
        assertThat(bestilling.fritekst()).isEqualTo(fritekst);
        assertThat(bestilling.revurderingÅrsak()).isNull();
        assertThat(bestilling.journalførSom()).isNull();
    }

    @Test
    void redigert_html_skal_være_null_hvis_en_ikke_finner_i_dokumentbehandling_entitet() {
        // Arrange
        var brevProdusertAvFpdokgen = "html";
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.genererHtml(behandling)).thenReturn(brevProdusertAvFpdokgen);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.empty());

        // Act
        var respons = brevRestTjeneste.hentOverstyringAvBrevMedOrginaltBrevPåHtmlFormat(new UuidDto(UUID.randomUUID()));

        // Assert
        var overstyrtDokumentDto = (BrevRestTjeneste.OverstyrtDokumentDto) respons.getEntity();
        assertThat(overstyrtDokumentDto.opprinneligHtml()).isEqualTo(brevProdusertAvFpdokgen);
        assertThat(overstyrtDokumentDto.redigertHtml()).isNull();
    }

    @Test
    void skal_returnere_opprinnelig_og_redigert_hvis_det_foreligger_overstyring() {
        // Arrange
        var brevProdusertAvFpdokgen = "html";
        var overstyryBrev = "OVERSTYRY BREV";
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.genererHtml(behandling)).thenReturn(brevProdusertAvFpdokgen);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.of(overstyryBrev));

        // Act
        var respons = brevRestTjeneste.hentOverstyringAvBrevMedOrginaltBrevPåHtmlFormat(new UuidDto(UUID.randomUUID()));

        // Assert
        var overstyrtDokumentDto = (BrevRestTjeneste.OverstyrtDokumentDto) respons.getEntity();
        assertThat(overstyrtDokumentDto.opprinneligHtml()).isEqualTo(brevProdusertAvFpdokgen);
        assertThat(overstyrtDokumentDto.redigertHtml()).isEqualTo(overstyryBrev);
    }

    @Test
    void skal_returnere_500_feil_hvis_opprinnelig_html_er_null_eller_tom() {
        // Arrange
        var brevProdusertAvFpdokgen = "";
        var overstyryBrev = "OVERSTYRY BREV";
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);
        when(dokumentForhåndsvisningTjenesteMock.genererHtml(behandling)).thenReturn(brevProdusertAvFpdokgen);
        when(dokumentBehandlingTjenesteMock.hentMellomlagretOverstyring(behandling.getId())).thenReturn(Optional.of(overstyryBrev));

        // Act
        var respons = brevRestTjeneste.hentOverstyringAvBrevMedOrginaltBrevPåHtmlFormat(new UuidDto(UUID.randomUUID()));
        assertThat(respons.getStatus()).isEqualTo(500);
    }

    @Test
    void mellomlagring_skal_lagre_innhold_hvis_oppgitt() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);

        // Act
        brevRestTjeneste.mellomlagringAvOverstyring(new BrevRestTjeneste.MellomlagreHtmlDto(behandling.getUuid(), "HTML"));

        var captorHtml = ArgumentCaptor.forClass(String.class);

        // Assert
        verify(dokumentBehandlingTjenesteMock).lagreOverstyrtBrev(any(), captorHtml.capture());

        assertThat(captorHtml.getValue()).isEqualTo("HTML");
    }

    @Test
    void mellomlagring_av_overstyring_brev_skal_fjerne_innhold_hvis_null() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagMocked();
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(behandling);

        // Act
        brevRestTjeneste.mellomlagringAvOverstyring(new BrevRestTjeneste.MellomlagreHtmlDto(behandling.getUuid(), null));

        // Assert
        verify(dokumentBehandlingTjenesteMock,  times(1)).fjernOverstyringAvBrev(any());
    }
}
