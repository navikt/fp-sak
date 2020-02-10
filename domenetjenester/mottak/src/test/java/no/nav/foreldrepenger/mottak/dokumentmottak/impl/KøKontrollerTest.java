package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
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
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private SøknadRepository søknadRepository;
    @Mock
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;

    private KøKontroller køKontroller;

    @Before
    public void oppsett() {
        MockitoAnnotations.initMocks(this);
        Mockito.spy(behandlingProsesseringTjeneste);
        when(behandlingRepositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(behandlingRepositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(behandlingRepositoryProvider.getYtelsesFordelingRepository()).thenReturn(ytelsesFordelingRepository);
        køKontroller = new KøKontroller(behandlingProsesseringTjeneste, behandlingRevurderingRepository, behandlingskontrollTjeneste,
            behandlingRepositoryProvider, historikkinnslagTjeneste, behandlingsoppretter);
    }

    @Test
    public void skal_ikke_oppdatere_behandling_med_henleggelse_når_original_behandling_er_siste_vedtak() {
        //Arrange
        Behandling morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        Behandling morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        Behandling farFgBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(farFgBehandling.getFagsak())).thenReturn(Optional.of(morKøetBehandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId())).thenReturn(Optional.of(morFgBehandling));

        //Act
        køKontroller.dekøFørsteBehandlingISakskompleks(farFgBehandling);

        //Assert
        Mockito.verify(behandlingsoppretter, times(0)).oppdaterBehandlingViaHenleggelse(any(), any());
        Mockito.verify(ytelsesFordelingRepository, times(0)).kopierGrunnlagFraEksisterendeBehandling(any(), any());
        Mockito.verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(eq(morKøetBehandling), any());
    }

    @Test
    public void skal_oppdatere_behandling_med_henleggelse_når_original_behandling_ikke_er_siste_vedtak_og_kopiere_ytelsesfordeling() {
        //Arrange
        Behandling morFgBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        Behandling morKøetBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        Behandling morBerørtBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morFgBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING).lagMocked();
        Behandling morOppdatertBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(morBerørtBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER).lagMocked();
        Behandling farFgBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(farFgBehandling.getFagsak())).thenReturn(Optional.of(morKøetBehandling));
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(morFgBehandling.getFagsakId())).thenReturn(Optional.of(morBerørtBehandling));
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(morKøetBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(morOppdatertBehandling);

        //Act
        køKontroller.dekøFørsteBehandlingISakskompleks(farFgBehandling);

        //Assert
        Mockito.verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(morKøetBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        Mockito.verify(ytelsesFordelingRepository).kopierGrunnlagFraEksisterendeBehandling(morKøetBehandling.getId(), morOppdatertBehandling.getId());
    }

    @Test
    public void skal_oprette_task_for_start_behandling_når_uten_køet_behandling(){
        //Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(behandling.getFagsak())).thenReturn(Optional.empty());
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.UDEFINERT)).thenReturn(behandling);

        //Act
        køKontroller.dekøFørsteBehandlingISakskompleks(behandling);

        //Assert
        Mockito.verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    public void skal_snike_i_kø_hvis_en_behandling_ligger_på_vent_pga_for_tidlig_søknad(){
        //Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagMocked();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD, null);
        Behandling behandlingMedforelder = scenario.lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(behandling.getFagsak())).thenReturn(Optional.empty());

        YtelseFordelingAggregat ytelse1 = lagYtelse(LocalDate.now());
        when(ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(ytelse1));
        YtelseFordelingAggregat ytelse2 = lagYtelse(LocalDate.now().plusMonths(4));
        when(ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingMedforelder.getId())).thenReturn(Optional.of(ytelse2));

        //Act
        boolean skalSnike = køKontroller.skalSnikeIKø(behandling.getFagsak(), behandlingMedforelder);

        //Assert
        assertThat(skalSnike).isTrue();
    }

    @Test
    public void skal_ikke_snike_i_kø_hvis_en_behandling_ligger_på_vent_pga_feks_fødsel(){
        //Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL, null);
        Behandling behandlingMedforelder = scenario.lagMocked();
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(behandling.getFagsak())).thenReturn(Optional.empty());

        //Act
        boolean skalSnike = køKontroller.skalSnikeIKø(behandling.getFagsak(), behandlingMedforelder);

        //Assert
        assertThat(skalSnike).isFalse();
    }

    private YtelseFordelingAggregat lagYtelse(LocalDate fomDato) {
        var ytelse = mock(YtelseFordelingAggregat.class);

        OppgittFordelingEntitet oppgittFordeling = mock(OppgittFordelingEntitet.class);
        OppgittPeriodeEntitet oppgittPeriode = mock(OppgittPeriodeEntitet.class);
        when(oppgittPeriode.getFom()).thenReturn(fomDato);
        when(oppgittFordeling.getOppgittePerioder()).thenReturn(List.of(oppgittPeriode));
        when(ytelse.getOppgittFordeling()).thenReturn(oppgittFordeling);
        return ytelse;
    }
}
