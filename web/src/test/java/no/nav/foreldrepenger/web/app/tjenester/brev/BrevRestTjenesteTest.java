package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.foreldrepenger.dokumentbestiller.DokumentMalType.INNHENTE_OPPLYSNINGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import no.nav.foreldrepenger.dokumentbestiller.DokumentForhåndsvisningTjeneste;

import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillDokumentDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

import org.mockito.ArgumentCaptor;

class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;
    private DokumentForhåndsvisningTjeneste dokumentForhåndsvisningTjenesteMock;
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    @BeforeEach
    public void setUp() {
        dokumentBestillerTjenesteMock = mock(DokumentBestillerTjeneste.class);
        dokumentForhåndsvisningTjenesteMock = mock(DokumentForhåndsvisningTjeneste.class);
        dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        arbeidsforholdInntektsmeldingMangelTjeneste = mock(ArbeidsforholdInntektsmeldingMangelTjeneste.class);

        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(dokumentForhåndsvisningTjenesteMock, dokumentBestillerTjenesteMock, dokumentBehandlingTjenesteMock, behandlingRepository, arbeidsforholdInntektsmeldingMangelTjeneste);
    }

    @Test
    void bestillerDokument() {
        // Arrange
        var behandlingUuid = UUID.randomUUID();
        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(mock(Behandling.class));
        var fritekst = "Dette er en fritekst";
        var dokumentMal = INNHENTE_OPPLYSNINGER;
        var bestillBrevDto = new BestillDokumentDto(behandlingUuid, dokumentMal, fritekst, null);

        // Act
        brevRestTjeneste.bestillDokument(bestillBrevDto);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentBestilling.class);

        // Assert
        verify(dokumentBestillerTjenesteMock).bestillDokument(bestillingCaptor.capture(), eq(HistorikkAktør.SAKSBEHANDLER));
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
    void harSendtVarselOmRevurdering() {
        // Arrange
        when(dokumentBehandlingTjenesteMock.erDokumentBestilt(any(), any())).thenReturn(true);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(mock(Behandling.class));

        // Act
        var harSendt = brevRestTjeneste.harSendtVarselOmRevurdering(new UuidDto(UUID.randomUUID()));

        // Assert
        verify(dokumentBehandlingTjenesteMock).erDokumentBestilt(any(), any());
        assertThat(harSendt).isTrue();
    }

}
