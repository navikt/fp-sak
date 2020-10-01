package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.foreldrepenger.dokumentbestiller.DokumentMalType.INNHENT_DOK;
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

import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;

public class BrevRestTjenesteTest {

    private BrevRestTjeneste brevRestTjeneste;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjenesteMock;
    private DokumentBehandlingTjeneste dokumentBehandlingTjenesteMock;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp() {
        dokumentBestillerApplikasjonTjenesteMock = mock(DokumentBestillerApplikasjonTjeneste.class);
        dokumentBehandlingTjenesteMock = mock(DokumentBehandlingTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.hentBehandling(anyLong())).thenReturn(mock(Behandling.class));

        brevRestTjeneste = new BrevRestTjeneste(dokumentBestillerApplikasjonTjenesteMock, dokumentBehandlingTjenesteMock, behandlingRepository);
    }

    @Test
    public void bestillerDokument() {
        // Arrange
        long behandlingId = 2L;
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandlingId, INNHENT_DOK, "Dette er en fritekst");

        // Act
        brevRestTjeneste.bestillDokument(bestillBrevDto);

        // Assert
        verify(dokumentBestillerApplikasjonTjenesteMock).bestillDokument(eq(bestillBrevDto), eq(HistorikkAktør.SAKSBEHANDLER), eq(true));
    }

    @Test
    public void harSendtVarselOmRevurdering() {
        // Arrange
        when(dokumentBehandlingTjenesteMock.erDokumentBestilt(any(), any())).thenReturn(true);
        when(behandlingRepository.hentBehandling(any(UUID.class))).thenReturn(mock(Behandling.class));

        // Act
        Boolean harSendt = brevRestTjeneste.harSendtVarselOmRevurdering(new UuidDto(UUID.randomUUID()));

        // Assert
        verify(dokumentBehandlingTjenesteMock).erDokumentBestilt(any(), any());
        assertThat(harSendt).isEqualTo(true);
    }
}
