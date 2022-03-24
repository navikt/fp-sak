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
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.dokumentbestiller.brevmal.BrevmalTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

public class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private DokumentBestillerTjeneste dokumentBestillerTjenesteMock;
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    private BehandlingRepository behandlingRepository;
    private BrevmalTjeneste brevmalTjeneste;

    @BeforeEach
    public void setUp() {
        dokumentBestillerTjenesteMock = mock(DokumentBestillerTjeneste.class);
        dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        brevmalTjeneste = new BrevmalTjeneste(dokumentBehandlingTjenesteMock);

        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(dokumentBestillerTjenesteMock, dokumentBehandlingTjenesteMock, behandlingRepository, brevmalTjeneste);
    }

    @Test
    public void bestillerDokument() {
        // Arrange
        var behandlingId = 2L;
        var bestillBrevDto = new BestillBrevDto(behandlingId, UUID.randomUUID(), INNHENTE_OPPLYSNINGER, "Dette er en fritekst");

        // Act
        brevRestTjeneste.bestillDokument(bestillBrevDto);

        // Assert
        verify(dokumentBestillerTjenesteMock).bestillDokument(eq(bestillBrevDto), eq(HistorikkAktør.SAKSBEHANDLER));
    }

    @Test
    public void harSendtVarselOmRevurdering() {
        // Arrange
        when(dokumentBehandlingTjenesteMock.erDokumentBestilt(any(), any())).thenReturn(true);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(mock(Behandling.class));

        // Act
        var harSendt = brevRestTjeneste.harSendtVarselOmRevurdering(new UuidDto(UUID.randomUUID()));

        // Assert
        verify(dokumentBehandlingTjenesteMock).erDokumentBestilt(any(), any());
        assertThat(harSendt).isTrue();
    }

    @Test
    public void hentMaler() {
        // Arrange
        when(dokumentBehandlingTjenesteMock.erDokumentBestilt(any(), any())).thenReturn(true);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(mock(Behandling.class));

        // Act
        var maler = brevRestTjeneste.hentMaler(new UuidDto(UUID.randomUUID()));

        // Assert
        assertThat(maler).isNotEmpty();
        assertThat(maler);
    }
}
