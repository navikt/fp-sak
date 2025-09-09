package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.EnhetsTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class PraksisendringAapSakTaskTest {
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private RevurderingTjeneste revurderingTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;

    private PraksisendringAapSakTask praksisendringAapSakTask;
    private ProsessTaskData data;
    private Behandling behandling;

    @BeforeEach
    void setup() {
        when(behandlingRepositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(behandlingRepositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        praksisendringAapSakTask = new PraksisendringAapSakTask(behandlingRepositoryProvider, behandlingProsesseringTjeneste, revurderingTjeneste);
        data = ProsessTaskData.forProsessTask(PraksisendringAapBatchTask.class);
        data.setFagsak("123", 1L);
        behandling = mock(Behandling.class);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
    }

    @Test
    void skal_opprette_revurdering_når_forrige_behandling_er_avsluttet() {
        // Arrange
        var revurdering = mock(Behandling.class);
        when(behandling.erAvsluttet()).thenReturn(true);
        var fagsak = mock(Fagsak.class);
        when(fagsakRepository.hentSakGittSaksnummer(new Saksnummer("123"))).thenReturn(Optional.of(fagsak));
        when(revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_BG_AAP_KOMBI, EnhetsTjeneste.MIDLERTIDIG_ENHET)).thenReturn(revurdering);

        // Act
        praksisendringAapSakTask.prosesser(data, 1L);

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(revurdering);
    }

    @Test
    void skal_ikke_gjøre_noe_når_det_er_åpen_førstegangsbehandling() {
        // Arrange
        when(behandling.erAvsluttet()).thenReturn(false);
        when(behandling.erRevurdering()).thenReturn(false);

        // Act
        praksisendringAapSakTask.prosesser(data, 1L);

        // Assert
        verifyNoInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    void skal_enkøe_behandling_ved_åpen_revurdering() {
        // Arrange
        var revurdering = mock(Behandling.class);
        when(behandling.erAvsluttet()).thenReturn(false);
        when(behandling.erRevurdering()).thenReturn(true);
        var fagsak = mock(Fagsak.class);
        when(fagsakRepository.hentSakGittSaksnummer(new Saksnummer("123"))).thenReturn(Optional.of(fagsak));
        when(revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_BG_AAP_KOMBI, EnhetsTjeneste.MIDLERTIDIG_ENHET)).thenReturn(revurdering);

        // Act
        praksisendringAapSakTask.prosesser(data, 1L);

        // Assert
        verify(behandlingProsesseringTjeneste).enkøBehandling(revurdering);
    }
}
