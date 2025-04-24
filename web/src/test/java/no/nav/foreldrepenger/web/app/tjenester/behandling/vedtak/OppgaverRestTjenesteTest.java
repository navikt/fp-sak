package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app.OppgaveDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.OppgaveDto;

class OppgaverRestTjenesteTest {

    private static final UuidDto UUID_DTO = new UuidDto(UUID.randomUUID());

    private final BehandlingRepository behandlingRepositoryMock = mock(BehandlingRepository.class);
    private final OppgaveDtoTjeneste oppgaveDtoTjenesteMock = mock(OppgaveDtoTjeneste.class);
    private final OppgaveTjeneste oppgaveTjenesteMock = mock(OppgaveTjeneste.class);
    private final Behandling behandling = mock(Behandling.class);
    private OppgaverRestTjeneste oppgaverRestTjeneste;

    @BeforeEach
    void setUp() {
        when(behandling.getUuid()).thenReturn(UUID_DTO.getBehandlingUuid());
        when(behandlingRepositoryMock.hentBehandling(any(UUID.class))).thenReturn(behandling);
        oppgaverRestTjeneste = new OppgaverRestTjeneste(behandlingRepositoryMock, oppgaveDtoTjenesteMock, oppgaveTjenesteMock);
    }

    @Test
    void skal_hente_oppgaver_for_foreldrepenger() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.FORELDREPENGER);
        oppgaverRestTjeneste.hentOppgaver(UUID_DTO);

        verify(oppgaveDtoTjenesteMock).mapTilDto(behandling.getAktørId());
    }

    @Test
    void skal_hente_oppgaver_for_engangsstønad() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        oppgaverRestTjeneste.hentOppgaver(UUID_DTO);

        verify(oppgaveDtoTjenesteMock).mapTilDto(behandling.getAktørId());
    }

    @Test
    void skal_hente_oppgaver_for_svangerskapspenger() {
        when(behandling.getFagsakYtelseType()).thenReturn(FagsakYtelseType.SVANGERSKAPSPENGER);
        oppgaverRestTjeneste.hentOppgaver(UUID_DTO);

        verify(oppgaveDtoTjenesteMock).mapTilDto(behandling.getAktørId());
    }

    @Test
    void skal_ferdigstille_oppgave() {
        var oppgaveId = "1";
        try (var response = oppgaverRestTjeneste.ferdigstillOppgave(new OppgaveDto.OppgaveId(oppgaveId))) {
            verify(oppgaveTjenesteMock).ferdigstillOppgave(oppgaveId);
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        }
    }
}
