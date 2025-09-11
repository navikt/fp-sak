package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.EnhetsTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

import java.util.Optional;

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

    @BeforeEach
    void setup() {
        when(behandlingRepositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(behandlingRepositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        praksisendringAapSakTask = new PraksisendringAapSakTask(behandlingRepositoryProvider, behandlingProsesseringTjeneste, revurderingTjeneste);
    }

    @Test
    void skal_opprette_revurdering_når_forrige_behandling_er_avsluttet() {
        // Arrange
        var data = ProsessTaskData.forProsessTask(PraksisendringAapBatchTask.class);
        data.setFagsak("123", 1L);
        var behandling = mock(Behandling.class);
        var revurdering = mock(Behandling.class);
        when(behandling.erAvsluttet()).thenReturn(true);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        var fagsak = mock(Fagsak.class);
        when(fagsakRepository.hentSakGittSaksnummer(new Saksnummer("123"))).thenReturn(Optional.of(fagsak));
        when(revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_BEREGNING_AAP_KOMBINASJON, EnhetsTjeneste.MIDLERTIDIG_ENHET)).thenReturn(revurdering);

        // Act
        praksisendringAapSakTask.prosesser(data, 1L);

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(revurdering);
    }

    @Test
    void skal_ikke_gjøre_noe_når_det_er_åpen_førstegangsbehandling() {
        // Arrange
        var data = ProsessTaskData.forProsessTask(PraksisendringAapBatchTask.class);
        data.setFagsak("123", 1L);
        var behandling = mock(Behandling.class);
        when(behandling.erAvsluttet()).thenReturn(false);
        when(behandling.getType()).thenReturn(BehandlingType.FØRSTEGANGSSØKNAD);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));

        // Act
        praksisendringAapSakTask.prosesser(data, 1L);

        // Assert
        verifyNoInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    void skal_enkøe_behandling_ved_åpen_spesialbehandling() {
        // Arrange
        var data = ProsessTaskData.forProsessTask(PraksisendringAapBatchTask.class);
        data.setFagsak("123", 1L);
        var behandling = mock(Behandling.class);
        var revurdering = mock(Behandling.class);
        when(behandling.erAvsluttet()).thenReturn(false);
        when(behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.alleTekniskeÅrsaker())).thenReturn(true);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        var fagsak = mock(Fagsak.class);
        when(fagsakRepository.hentSakGittSaksnummer(new Saksnummer("123"))).thenReturn(Optional.of(fagsak));
        when(revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_BEREGNING_AAP_KOMBINASJON, EnhetsTjeneste.MIDLERTIDIG_ENHET)).thenReturn(revurdering);

        // Act
        praksisendringAapSakTask.prosesser(data, 1L);

        // Assert
        verify(behandlingProsesseringTjeneste).enkøBehandling(revurdering);
    }

    @Test
    void skal_rulle_tilbake_åpne_behandlinger() {
        // Arrange
        var data = ProsessTaskData.forProsessTask(PraksisendringAapBatchTask.class);
        data.setFagsak("123", 1L);
        var behandling = mock(Behandling.class);
        var lås = mock(BehandlingLås.class);
        when(behandling.erAvsluttet()).thenReturn(false);
        when(behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.alleTekniskeÅrsaker())).thenReturn(false);
        when(behandling.getStartpunkt()).thenReturn(StartpunktType.UTTAKSVILKÅR);
        when(behandling.getType()).thenReturn(BehandlingType.REVURDERING);
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.taSkriveLås(behandling)).thenReturn(lås);

        // Act
        praksisendringAapSakTask.prosesser(data, 1L);

        // Assert
        verify(behandlingProsesseringTjeneste).reposisjonerBehandlingTilbakeTil(behandling, lås, BehandlingStegType.DEKNINGSGRAD);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

}
