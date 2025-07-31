package no.nav.foreldrepenger.mottak.sakskompleks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class KøKontrollerTest {
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Mock
    private SøknadRepository søknadRepository;
    @Mock
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private BehandlingFlytkontroll flytkontroll;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    private KøKontroller køKontroller;

    @BeforeEach
    void oppsett() {
        when(behandlingRepositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(behandlingRepositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(behandlingRepositoryProvider.getYtelsesFordelingRepository()).thenReturn(ytelsesFordelingRepository);
        køKontroller = new KøKontroller(behandlingProsesseringTjeneste, behandlingRepositoryProvider, taskTjeneste,
            behandlingRevurderingTjeneste, behandlingsoppretter, flytkontroll);
    }

    @Test
    void skal_ikke_oppdatere_behandling_med_henleggelse_når_original_behandling_er_siste_vedtak() {
        // Arrange
        var morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        var farFgBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(farFgBehandling.getFagsak())).thenReturn(Optional.of(morKøetBehandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId())).thenReturn(Optional.of(morFgBehandling));

        // Act
        køKontroller.dekøFørsteBehandlingISakskompleks(farFgBehandling);

        // Assert
        Mockito.verify(behandlingsoppretter, times(0)).oppdaterBehandlingViaHenleggelse(any(), any());
        Mockito.verify(ytelsesFordelingRepository, times(0)).kopierGrunnlagFraEksisterendeBehandling(any(), any());
        Mockito.verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(eq(morKøetBehandling), any());
    }

    @Test
    void skal_oppdatere_behandling_med_henleggelse_når_original_behandling_ikke_er_siste_vedtak_og_kopiere_ytelsesfordeling() {
        // Arrange
        var morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        var morBerørtBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING).lagMocked();
        var morOppdatertBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morBerørtBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        var farFgBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(farFgBehandling.getFagsak())).thenReturn(Optional.of(morKøetBehandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId()))
                .thenReturn(Optional.of(morBerørtBehandling));
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(morKøetBehandling))
                .thenReturn(morOppdatertBehandling);

        // Act
        køKontroller.dekøFørsteBehandlingISakskompleks(farFgBehandling);

        // Assert
        Mockito.verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(morKøetBehandling);
        Mockito.verify(ytelsesFordelingRepository).kopierGrunnlagFraEksisterendeBehandling(morKøetBehandling.getId(), morOppdatertBehandling);
    }

    @Test
    void skal_oprette_task_for_start_behandling_når_uten_køet_behandling() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(behandling.getFagsak())).thenReturn(Optional.empty());

        // Act
        køKontroller.dekøFørsteBehandlingISakskompleks(behandling);

        // Assert
        Mockito.verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    void skal_starte_hvis_en_2part_behandling_ligger_før_uttak() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagMocked();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.lagMocked();
        // Act
        var skalKøes = køKontroller.skalEvtNyBehandlingKøes(behandling.getFagsak());

        // Assert
        assertThat(skalKøes).isFalse();
    }

    @Test
    void skal_ikke_starte_hvis_en_2part_behandling_ligger_til_vedtak() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.lagMocked();
        when(flytkontroll.nyRevurderingSkalVente(behandling.getFagsak())).thenReturn(true);
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));

        // Act
        var skalKøes = køKontroller.skalEvtNyBehandlingKøes(behandling.getFagsak());

        // Assert
        assertThat(skalKøes).isTrue();
    }

    @Test
    void sakskompleks_lagre_oppdater_når_original_behandling_ikke_er_siste_vedtak_og_kopiere_ytelsesfordeling() {
        // Arrange
        var morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        morKøetBehandling.setOpprettetTidspunkt(LocalDateTime.now().minusHours(1));
        var morBerørtBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING).lagMocked();
        var farFgBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(farFgBehandling.getFagsak())).thenReturn(Optional.of(morKøetBehandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId()))
            .thenReturn(Optional.of(morBerørtBehandling));

        // Act
        køKontroller.håndterSakskompleks(farFgBehandling.getFagsak());

        // Assert
        Mockito.verify(taskTjeneste).lagre(any(ProsessTaskData.class));
    }

    @Test
    void sakskompleks_skal_oppdatere_når_original_behandling_ikke_er_siste_vedtak_og_kopiere_ytelsesfordeling() {
        // Arrange
        var morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        var morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        var morBerørtBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING).lagMocked();
        var morOppdatertBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morBerørtBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId()))
            .thenReturn(Optional.of(morBerørtBehandling));
        when(behandlingRepository.hentBehandling(morKøetBehandling.getId())).thenReturn(morKøetBehandling);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(morKøetBehandling))
            .thenReturn(morOppdatertBehandling);

        // Act
        køKontroller.oppdaterVedHenleggelseOmNødvendigOgFortsettBehandling(morKøetBehandling.getId());

        // Assert
        Mockito.verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(morKøetBehandling);
        Mockito.verify(ytelsesFordelingRepository).kopierGrunnlagFraEksisterendeBehandling(morKøetBehandling.getId(), morOppdatertBehandling);
    }

}
