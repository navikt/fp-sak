package no.nav.foreldrepenger.mottak.sakskompleks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandling.revurdering.BerørtBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.fp.BeregnFeriepenger;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class BerørtBehandlingKontrollerTest {

    private BerørtBehandlingKontroller berørtBehandlingKontroller;

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;
    @Mock
    private FpUttakRepository fpUttakRepository;
    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private BerørtBehandlingTjeneste berørtBehandlingTjeneste;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Mock
    private SøknadRepository søknadRepository;
    @Mock
    private BeregnFeriepenger beregnFeriepenger;

    private Fagsak fagsak;
    private Fagsak fagsakMedforelder;
    private Behandling fBehandling;
    private Behandling køetBehandling;
    private Behandling fBehandlingMedforelder;
    private Behandling køetBehandlingMedforelder;
    private Behandling berørt;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        behandlingProsesseringTjeneste = spy(behandlingProsesseringTjeneste);
        prosessTaskRepository = spy(prosessTaskRepository);
        berørtBehandlingTjeneste = spy(berørtBehandlingTjeneste);
        behandlingsoppretter = spy(behandlingsoppretter);
        FagsakLåsRepository fagsakLåsRepository = mock(FagsakLåsRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingRevurderingRepository()).thenReturn(behandlingRevurderingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getFpUttakRepository()).thenReturn(fpUttakRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        when(repositoryProvider.getYtelsesFordelingRepository()).thenReturn(ytelsesFordelingRepository);
        when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(repositoryProvider.getBeregningsresultatRepository()).thenReturn(beregningsresultatRepository);


        fBehandling = lagBehandling();
        fagsak = fBehandling.getFagsak();
        køetBehandling = lagRevurdering(fBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        berørt = lagRevurdering(fBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        fBehandlingMedforelder = lagBehandling();
        fagsakMedforelder = fBehandlingMedforelder.getFagsak();
        køetBehandlingMedforelder = lagRevurdering(fBehandlingMedforelder, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        Behandling berørtMedforelder = lagRevurdering(fBehandlingMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);

        when(behandlingRepository.hentBehandling(fBehandling.getId())).thenReturn(fBehandling);
        when(behandlingRepository.hentBehandling(fBehandlingMedforelder.getId())).thenReturn(fBehandlingMedforelder);
        when(behandlingRepository.hentBehandling(berørt.getId())).thenReturn(berørt);
        when(behandlingRepository.hentBehandling(køetBehandling.getId())).thenReturn(køetBehandling);
        when(behandlingRepository.hentBehandling(køetBehandlingMedforelder.getId())).thenReturn(køetBehandlingMedforelder);
        when(behandlingRepository.hentBehandling(berørtMedforelder.getId())).thenReturn(berørtMedforelder);
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakMedforelder.getId())).thenReturn(List.of(køetBehandlingMedforelder));

        when(behandlingsresultatRepository.hent(fBehandling.getId())).thenReturn(Behandlingsresultat.builder().build());

        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakMedforelder));
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsakMedforelder)).thenReturn(Optional.of(fagsak));
        when(behandlingsoppretter.opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(berørtMedforelder);
        when(behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(berørt);

        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(eq(køetBehandling), any(BehandlingÅrsakType.class))).thenReturn(køetBehandling);

        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(Optional.empty());


        var køkontroller = new KøKontroller(behandlingProsesseringTjeneste,
            behandlingskontrollTjeneste, repositoryProvider, behandlingsoppretter, null);
        berørtBehandlingKontroller = new BerørtBehandlingKontroller(repositoryProvider, berørtBehandlingTjeneste, behandlingsoppretter, beregnFeriepenger, køkontroller);
    }

    @Test
    public void testHåndterEgenKø() { // Vurder innhold - vil pt ikke være kø når ukoblet
        // Arrange
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(
            Optional.of(køetBehandling));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandling,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testHåndterKøHvisFørstegangUttakKø() {
        // Arrange
        Behandling køetBehandlingPåVent = mock(Behandling.class);
        Aksjonspunkt aksjonspunkt = mock(Aksjonspunkt.class);
        AktørId aktørId = mock(AktørId.class);
        var nå = LocalDateTime.now();
        when(aktørId.getId()).thenReturn("");
        when(køetBehandlingPåVent.getFagsakId()).thenReturn(fagsakMedforelder.getId());
        when(køetBehandlingPåVent.getId()).thenReturn(køetBehandlingMedforelder.getId());
        when(køetBehandlingPåVent.getAktørId()).thenReturn(aktørId);
        when(køetBehandlingPåVent.getOpprettetTidspunkt()).thenReturn(nå);

        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingPåVent));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.of(køetBehandlingPåVent));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(any(), any());
    }

    @Test
    public void testBerørtBehandlingMedforelder() {
        // Arrange
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingMedforelder));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.of(køetBehandlingMedforelder));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testBerørtBehandlingMedforelderNårMedforelderHarKøetBehandlingAllerede() {
        // Arrange
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId())).thenReturn(
            Optional.of(fBehandlingMedforelder));
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(
            lagBehandlingsresultat(fBehandling, BehandlingResultatType.OPPHØR,
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(
            Optional.of(køetBehandlingMedforelder));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    private void settOppAvsluttetBehandlingBruker() {
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(
            Optional.of(fBehandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(
            lagBehandlingsresultatInnvilget(fBehandling));
    }

    private void settOppAvsluttetBehandlingAnnenpart() {
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId())).thenReturn(
            Optional.of(fBehandlingMedforelder));
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(
            lagBehandlingsresultatInnvilget(fBehandlingMedforelder));
    }

    private void settOppKøBruker() {
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(
            Optional.of(køetBehandling));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(
            Optional.of(køetBehandling));
    }

    private void settOppKøAnnenpart() {
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(
            Optional.of(køetBehandlingMedforelder));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsak)).thenReturn(
            Optional.of(køetBehandlingMedforelder));
    }

    private Behandling lagBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var behandling = scenario.lagMocked();
        behandling.setOpprettetTidspunkt(LocalDateTime.now());
        return behandling;
    }

    private Behandling lagRevurdering(Behandling behandling, BehandlingÅrsakType årsakType) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(behandling, årsakType)
            .medBehandlingType(BehandlingType.REVURDERING);
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medFagsakId(behandling.getId()).medSaksnummer(behandling.getFagsak().getSaksnummer());
        var revurdering = scenario.lagMocked();
        revurdering.setOpprettetTidspunkt(LocalDateTime.now());
        return revurdering;
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling,
                                                                 BehandlingResultatType behandlingResultatType,
                                                                 KonsekvensForYtelsen konsekvensForYtelsen) {
        return Optional.of(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen)
            .buildFor(behandling));
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultatInnvilget(Behandling behandling) {
        return Optional.of(Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling));
    }


    @Test
    public void køHosBeggeParterSkalIkkeOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        settOppKøAnnenpart();
        køetBehandling.setOpprettetTidspunkt(LocalDateTime.now());
        when(
            berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(
            false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void køHosAnnenpartSkalOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøAnnenpart();

        when(
            berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(
            true);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - oppretter berørt behandling på medforelder
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
        verifyNoMoreInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    public void køBrukerSkalIkkeOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        when(
            berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(
            false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandling,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void ingenKøSkalOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        when(
            berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(
            true);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert opprett berørt (for medforelder)
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    public void køBeggeParterSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        settOppKøAnnenpart();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - dekø fra medforelders kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandling,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void køAnnenpartSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøAnnenpart();

        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert  - dekø fra medforelders kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void køBrukerSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        when(
            berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(
            false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert dekø fra egen kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandling,
            Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void ingenKøSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        when(
            berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(
            false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - skal ikke skje noe
        verifyZeroInteractions(behandlingsoppretter);
        verifyZeroInteractions(behandlingProsesseringTjeneste);
    }
}

