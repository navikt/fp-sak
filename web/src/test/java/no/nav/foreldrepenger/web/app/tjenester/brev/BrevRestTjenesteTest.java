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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp() {
        dokumentBestillerTjenesteMock = mock(DokumentBestillerTjeneste.class);
        dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);

        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(dokumentBestillerTjenesteMock, dokumentBehandlingTjenesteMock, behandlingRepository);
    }

    @Test
    void bestillerDokument() {
        // Arrange
        var behandlingUuid = UUID.randomUUID();

        when(behandlingRepository.hentBehandling(behandlingUuid)).thenReturn(mock(Behandling.class));

        var bestillBrevDto = new BestillBrevDto(behandlingUuid, INNHENTE_OPPLYSNINGER, "Dette er en fritekst");

        // Act
        brevRestTjeneste.bestillDokument(bestillBrevDto);

        // Assert
        verify(dokumentBestillerTjenesteMock).bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER);
        verify(behandlingRepository).hentBehandling(behandlingUuid);
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
