package no.nav.foreldrepenger.mottak.sakskompleks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.BerørtBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.ytelse.beregning.fp.BeregnFeriepenger;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BerørtBehandlingKontrollerTest {

    private BerørtBehandlingKontroller berørtBehandlingKontroller;

    @Mock
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private HistorikkinnslagRepository historikkinnslagRepository;
    @Mock
    private BerørtBehandlingTjeneste berørtBehandlingTjeneste;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
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
    private Behandling berørtFeriepenger;

    @BeforeEach
    void setUp() {

        clearInvocations(behandlingsoppretter, behandlingProsesseringTjeneste);

        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        var fagsakLåsRepository = mock(FagsakLåsRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(repositoryProvider.getHistorikkinnslagRepository()).thenReturn(historikkinnslagRepository);


        fBehandling = lagBehandling();
        fagsak = fBehandling.getFagsak();
        køetBehandling = lagRevurdering(fBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        berørt = lagRevurdering(fBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        fBehandlingMedforelder = lagBehandling();
        fagsakMedforelder = fBehandlingMedforelder.getFagsak();
        køetBehandlingMedforelder = lagRevurdering(fBehandlingMedforelder, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        var berørtMedforelder = lagRevurdering(fBehandlingMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        berørtFeriepenger = lagRevurdering(fBehandlingMedforelder,
            List.of(BehandlingÅrsakType.BERØRT_BEHANDLING, BehandlingÅrsakType.REBEREGN_FERIEPENGER));

        when(behandlingRepository.hentBehandling(fBehandling.getId())).thenReturn(fBehandling);
        when(behandlingRepository.hentBehandling(fBehandlingMedforelder.getId())).thenReturn(fBehandlingMedforelder);
        when(behandlingRepository.hentBehandling(berørt.getId())).thenReturn(berørt);
        when(behandlingRepository.hentBehandling(køetBehandling.getId())).thenReturn(køetBehandling);
        when(behandlingRepository.hentBehandling(køetBehandlingMedforelder.getId())).thenReturn(køetBehandlingMedforelder);
        when(behandlingRepository.hentBehandling(berørtMedforelder.getId())).thenReturn(berørtMedforelder);
        when(behandlingRepository.hentBehandling(berørtFeriepenger.getId())).thenReturn(berørtFeriepenger);
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakMedforelder.getId())).thenReturn(List.of(køetBehandlingMedforelder));

        when(behandlingsresultatRepository.hent(fBehandling.getId())).thenReturn(Behandlingsresultat.builder().build());

        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakMedforelder));
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsakMedforelder)).thenReturn(Optional.of(fagsak));
        when(behandlingsoppretter.opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(berørtMedforelder);
        when(behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(berørt);

        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(eq(køetBehandling), any(BehandlingÅrsakType.class))).thenReturn(køetBehandling);

        when(behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.empty());
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.empty());
        when(behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(Optional.empty());
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(Optional.empty());

        when(ytelseFordelingTjeneste.hentAggregat(anyLong())).thenReturn(YtelseFordelingAggregat.oppdatere(Optional.empty()).medSakskompleksDekningsgrad(
            Dekningsgrad._100).build());

        var køkontroller = new KøKontroller(behandlingProsesseringTjeneste, repositoryProvider, null, behandlingRevurderingTjeneste,
            behandlingsoppretter, null);
        berørtBehandlingKontroller =
            new BerørtBehandlingKontroller(repositoryProvider, behandlingRevurderingTjeneste, berørtBehandlingTjeneste, behandlingsoppretter,
            beregnFeriepenger, køkontroller, ytelseFordelingTjeneste, behandlingProsesseringTjeneste);
    }

    @Test
    void testHåndterEgenKø() { // Vurder innhold - vil pt ikke være kø når ukoblet
        // Arrange
        when(behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.empty());
        when(behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(Optional.of(køetBehandling));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandling);
    }

    @Test
    void testHåndterKøHvisFørstegangUttakKø() {
        // Arrange
        var køetBehandlingPåVent = mock(Behandling.class);
        var aktørId = mock(AktørId.class);
        var nå = LocalDateTime.now();
        when(aktørId.getId()).thenReturn("");
        when(køetBehandlingPåVent.getFagsakId()).thenReturn(fagsakMedforelder.getId());
        when(køetBehandlingPåVent.getId()).thenReturn(køetBehandlingMedforelder.getId());
        when(køetBehandlingPåVent.getAktørId()).thenReturn(aktørId);
        when(køetBehandlingPåVent.getOpprettetTidspunkt()).thenReturn(nå);

        when(behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingPåVent));
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.of(køetBehandlingPåVent));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).dekøBehandling(any());
    }

    @Test
    void testBerørtBehandlingMedforelder() {
        // Arrange
        when(behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingMedforelder));
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.of(køetBehandlingMedforelder));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandlingMedforelder);
    }

    @Test
    void testBerørtBehandlingMedforelderNårMedforelderHarKøetBehandlingAllerede() {
        // Arrange
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId())).thenReturn(
            Optional.of(fBehandlingMedforelder));
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(
            lagBehandlingsresultat(fBehandling, BehandlingResultatType.OPPHØR, KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER));
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(Optional.of(køetBehandlingMedforelder));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verifyNoMoreInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandlingMedforelder);
    }

    private void settOppAvsluttetBehandlingBruker() {
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(fBehandling));
        var br = lagBehandlingsresultatInnvilget(fBehandling);
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(Optional.of(br));
        when(behandlingsresultatRepository.hent(fBehandling.getId())).thenReturn(br);
    }

    private void settOppAvsluttetBehandlingAnnenpart() {
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId())).thenReturn(
            Optional.of(fBehandlingMedforelder));
        var br = lagBehandlingsresultatInnvilget(fBehandlingMedforelder);
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandlingMedforelder.getId())).thenReturn(Optional.of(br));
        when(behandlingsresultatRepository.hent(fBehandlingMedforelder.getId())).thenReturn(br);
    }

    private void settOppKøBruker() {
        when(behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(Optional.of(køetBehandling));
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(Optional.of(køetBehandling));
    }

    private void settOppKøAnnenpart() {
        when(behandlingRevurderingTjeneste.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingMedforelder));
        when(behandlingRevurderingTjeneste.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.of(køetBehandlingMedforelder));
    }

    private Behandling lagBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var behandling = scenario.lagMocked();
        behandling.setOpprettetTidspunkt(LocalDateTime.now());
        return behandling;
    }

    private Behandling lagRevurdering(Behandling behandling, BehandlingÅrsakType årsakType) {
        return lagRevurdering(behandling, List.of(årsakType));
    }

    private Behandling lagRevurdering(Behandling behandling, List<BehandlingÅrsakType> årsakType) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(behandling, årsakType, false)
            .medBehandlingType(BehandlingType.REVURDERING);
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medFagsakId(behandling.getId()).medSaksnummer(behandling.getSaksnummer());
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

    private Behandlingsresultat lagBehandlingsresultatInnvilget(Behandling behandling) {
        return Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
    }


    @Test
    void køHosBeggeParterSkalIkkeOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        settOppKøAnnenpart();
        køetBehandling.setOpprettetTidspunkt(LocalDateTime.now());
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert
        verifyNoMoreInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandlingMedforelder);
    }

    @Test
    void køHosAnnenpartSkalOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøAnnenpart();

        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class)))
            .thenReturn(Optional.of(BerørtBehandlingTjeneste.BerørtÅrsak.ORDINÆR));
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - oppretter berørt behandling på medforelder
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
        verifyNoMoreInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    void køBrukerSkalIkkeOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert
        verifyNoMoreInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandling);
    }

    @Test
    void ingenKøSkalOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class)))
            .thenReturn(Optional.of(BerørtBehandlingTjeneste.BerørtÅrsak.ORDINÆR));
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert opprett berørt (for medforelder)
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    void ingenKøFinnesBerørtSkalIkkeOppretteBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class)))
            .thenReturn(Optional.of(BerørtBehandlingTjeneste.BerørtÅrsak.ORDINÆR));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(List.of(berørt));
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandlingMedforelder.getId());
        // Assert opprett berørt (for medforelder)
        verifyNoInteractions(behandlingsoppretter);
        verifyNoInteractions(behandlingProsesseringTjeneste);
    }


    @Test
    void ingenKøSkalOppretteFerieBerørt() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        when(beregnFeriepenger.avvikBeregnetFeriepengerBeregningsresultat(any())).thenReturn(true);
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(List.of());
        when(behandlingsoppretter.opprettRevurdering(any(), eq(BehandlingÅrsakType.REBEREGN_FERIEPENGER))).thenReturn(berørtFeriepenger);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert opprett berørt (for medforelder)
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.REBEREGN_FERIEPENGER);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    void skalOpppretteDekningsgradRevurdering() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(List.of());
        when(behandlingsoppretter.opprettRevurdering(any(), eq(BehandlingÅrsakType.ENDRE_DEKNINGSGRAD))).thenReturn(berørtFeriepenger);
        when(ytelseFordelingTjeneste.hentAggregat(fBehandling.getId())).thenReturn(
            YtelseFordelingAggregat.oppdatere(Optional.empty()).medSakskompleksDekningsgrad(Dekningsgrad._100).build());
        when(ytelseFordelingTjeneste.hentAggregat(fBehandlingMedforelder.getId())).thenReturn(
            YtelseFordelingAggregat.oppdatere(Optional.empty()).medSakskompleksDekningsgrad(Dekningsgrad._80).build());
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert opprett revurdering (for medforelder)
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.ENDRE_DEKNINGSGRAD);
    }

    @Test
    void køBeggeParterSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        settOppKøAnnenpart();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - dekø fra medforelders kø
        verifyNoMoreInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandling);
    }

    @Test
    void køAnnenpartSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøAnnenpart();

        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert  - dekø fra medforelders kø
        verifyNoMoreInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandlingMedforelder);
    }

    @Test
    void køBrukerSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        settOppKøBruker();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert dekø fra egen kø
        verifyNoMoreInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).dekøBehandling(køetBehandling);
    }

    @Test
    void ingenKøSkalIkkeOppretteBerørtHvisIkkeRelevant() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandlingAnnenpart();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Behandling.class), any(Long.class))).thenReturn(Optional.empty());
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - skal ikke skje noe
        verifyNoMoreInteractions(behandlingsoppretter);
        verifyNoMoreInteractions(behandlingProsesseringTjeneste);
    }
}

