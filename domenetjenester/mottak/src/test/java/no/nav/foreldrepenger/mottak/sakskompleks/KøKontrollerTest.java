package no.nav.foreldrepenger.mottak.sakskompleks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;

@ExtendWith(MockitoExtension.class)
public class KøKontrollerTest {
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private SøknadRepository søknadRepository;
    @Mock
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private BehandlingFlytkontroll flytkontroll;

    private KøKontroller køKontroller;

    @BeforeEach
    public void oppsett() {
        Mockito.spy(behandlingProsesseringTjeneste);
        when(behandlingRepositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(behandlingRepositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(behandlingRepositoryProvider.getYtelsesFordelingRepository()).thenReturn(ytelsesFordelingRepository);
        when(behandlingRepositoryProvider.getBehandlingRevurderingRepository()).thenReturn(behandlingRevurderingRepository);
        køKontroller = new KøKontroller(behandlingProsesseringTjeneste, behandlingskontrollTjeneste,
                behandlingRepositoryProvider, behandlingsoppretter, flytkontroll);
    }

    @Test
    public void skal_ikke_oppdatere_behandling_med_henleggelse_når_original_behandling_er_siste_vedtak() {
        // Arrange
        Behandling morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        Behandling morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        Behandling farFgBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(farFgBehandling.getFagsak())).thenReturn(Optional.of(morKøetBehandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId())).thenReturn(Optional.of(morFgBehandling));
        when(behandlingRepository.hentBehandling(morKøetBehandling.getId())).thenReturn(morKøetBehandling);

        // Act
        køKontroller.dekøFørsteBehandlingISakskompleks(farFgBehandling);

        // Assert
        Mockito.verify(behandlingsoppretter, times(0)).oppdaterBehandlingViaHenleggelse(any(), any());
        Mockito.verify(ytelsesFordelingRepository, times(0)).kopierGrunnlagFraEksisterendeBehandling(any(), any());
        Mockito.verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(eq(morKøetBehandling), any());
    }

    @Test
    public void skal_oppdatere_behandling_med_henleggelse_når_original_behandling_ikke_er_siste_vedtak_og_kopiere_ytelsesfordeling() {
        // Arrange
        Behandling morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        Behandling morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        Behandling morBerørtBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING).lagMocked();
        Behandling morOppdatertBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(morBerørtBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        Behandling farFgBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(farFgBehandling.getFagsak())).thenReturn(Optional.of(morKøetBehandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId()))
                .thenReturn(Optional.of(morBerørtBehandling));
        when(behandlingRepository.hentBehandling(morKøetBehandling.getId())).thenReturn(morKøetBehandling);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(morKøetBehandling))
                .thenReturn(morOppdatertBehandling);

        // Act
        køKontroller.dekøFørsteBehandlingISakskompleks(farFgBehandling);

        // Assert
        Mockito.verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(morKøetBehandling);
        Mockito.verify(ytelsesFordelingRepository).kopierGrunnlagFraEksisterendeBehandling(morKøetBehandling.getId(), morOppdatertBehandling.getId());
    }

    @Test
    public void skal_oprette_task_for_start_behandling_når_uten_køet_behandling() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(behandling.getFagsak())).thenReturn(Optional.empty());

        // Act
        køKontroller.dekøFørsteBehandlingISakskompleks(behandling);

        // Assert
        Mockito.verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    public void skal_starte_hvis_en_2part_behandling_ligger_før_uttak() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagMocked();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.lagMocked();
        // Act
        boolean skalKøes = køKontroller.skalEvtNyBehandlingKøes(behandling.getFagsak());

        // Assert
        assertThat(skalKøes).isFalse();
    }

    @Test
    public void skal_ikke_starte_hvis_en_2part_behandling_ligger_til_vedtak() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.lagMocked();
        when(flytkontroll.nyRevurderingSkalVente(behandling.getFagsak())).thenReturn(true);
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));

        // Act
        boolean skalKøes = køKontroller.skalEvtNyBehandlingKøes(behandling.getFagsak());

        // Assert
        assertThat(skalKøes).isTrue();
    }

}
