package no.nav.foreldrepenger.mottak.vedtak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

public class BerørtBehandlingKontrollerTest {

    private BerørtBehandlingKontroller berørtBehandlingKontroller;

    @Inject
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

    private Fagsak fagsak;
    private Fagsak fagsakMedforelder;
    private Behandling fBehandling;
    private Behandling køetBehandling;
    private Behandling fBehandlingMedforelder;
    private Behandling køetBehandlingMedforelder;
    private Behandling berørt;
    private Behandling berørtMedforelder;

    @Before
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

        berørtBehandlingKontroller = new BerørtBehandlingKontroller(repositoryProvider, behandlingProsesseringTjeneste, berørtBehandlingTjeneste, behandlingskontrollTjeneste, behandlingsoppretter);

        fBehandling = lagBehandling();
        fagsak = fBehandling.getFagsak();
        køetBehandling = lagRevurdering(fBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        berørt = lagRevurdering(fBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        fBehandlingMedforelder = lagBehandling();
        fagsakMedforelder = fBehandlingMedforelder.getFagsak();
        køetBehandlingMedforelder = lagRevurdering(fBehandlingMedforelder, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        berørtMedforelder = lagRevurdering(fBehandlingMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);

        when(behandlingRepository.hentBehandling(fBehandling.getId())).thenReturn(fBehandling);
        when(behandlingRepository.hentBehandling(fBehandlingMedforelder.getId())).thenReturn(fBehandlingMedforelder);
        when(behandlingRepository.hentBehandling(berørt.getId())).thenReturn(berørt);
        when(behandlingRepository.hentBehandling(køetBehandling.getId())).thenReturn(køetBehandling);
        when(behandlingRepository.hentBehandling(køetBehandlingMedforelder.getId())).thenReturn(køetBehandlingMedforelder);
        when(behandlingRepository.hentBehandling(berørtMedforelder.getId())).thenReturn(berørtMedforelder);

        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakMedforelder));
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsakMedforelder)).thenReturn(Optional.of(fagsak));
        when(behandlingsoppretter.opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(berørtMedforelder);
        when(behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(berørt);

        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(eq(køetBehandling), any(BehandlingÅrsakType.class))).thenReturn(køetBehandling);

        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(Optional.empty());

    }

    @Test
    public void testHåndterEgenKø() { // Vurder innhold - vil pt ikke være kø når ukoblet
        // Arrange
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.empty());
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(Optional.of(køetBehandling));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandling, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testHåndterKøHvisDetErSneketIKø() {
        // Arrange
        Behandling køetBehandlingPåVent = mock(Behandling.class);
        Aksjonspunkt aksjonspunkt = mock(Aksjonspunkt.class);
        AktørId aktørId = mock(AktørId.class);
        when(aktørId.getId()).thenReturn("");
        when(aksjonspunkt.erUtført()).thenReturn(true);
        when(aksjonspunkt.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD);
        when(aksjonspunkt.getFristTid()).thenReturn(LocalDateTime.now().plusDays(30));
        when(køetBehandlingPåVent.getAksjonspunkter()).thenReturn(Set.of(aksjonspunkt));
        when(køetBehandlingPåVent.getAksjonspunktFor(AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD)).thenReturn(aksjonspunkt);
        when(køetBehandlingPåVent.getFagsakId()).thenReturn(fagsakMedforelder.getId());
        when(køetBehandlingPåVent.getId()).thenReturn(køetBehandlingMedforelder.getId());
        when(køetBehandlingPåVent.getAktørId()).thenReturn(aktørId);

        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingPåVent));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterUtført(any(), any(), any(), anyString());
        verify(behandlingskontrollTjeneste).lagreAksjonspunkterReåpnet(any(), any(), eq(Boolean.TRUE), anyBoolean());
        verify(berørtBehandlingTjeneste).opprettHistorikkinnslagForVenteFristRelaterteInnslag(any(), any(), any(), any());
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    public void testBerørtBehandlingMedforelder() {
        // Arrange
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingMedforelder));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testBerørtBehandlingMedforelderNårMedforelderHarKøetBehandlingAllerede() {
        // Arrange
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(fBehandlingMedforelder));
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(lagBehandlingsresultat(fBehandling, BehandlingResultatType.OPPHØR,
            KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(Optional.of(køetBehandlingMedforelder));

        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());

        // Assert
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    private void settOppAvsluttetBehandlingBruker() {
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(fBehandling));
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(lagBehandlingsresultatInnvilget(fBehandling));
    }

    private void settOppAvsluttetBehandling2part() {
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(fBehandlingMedforelder));
        when(behandlingsresultatRepository.hentHvisEksisterer(fBehandling.getId())).thenReturn(lagBehandlingsresultatInnvilget(fBehandlingMedforelder));
    }

    private void settOppAvsluttetBerørt() {
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())).thenReturn(Optional.of(berørt));
        when(behandlingsresultatRepository.hentHvisEksisterer(berørt.getId())).thenReturn(lagBehandlingsresultatInnvilget(berørt));
    }

    private void settOppKøBruker() {
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsak.getId())).thenReturn(Optional.of(køetBehandling));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsakMedforelder)).thenReturn(Optional.of(køetBehandling));
    }

    private void settOppKø2Part() {
        when(behandlingRevurderingRepository.finnKøetYtelsesbehandling(fagsakMedforelder.getId())).thenReturn(Optional.of(køetBehandlingMedforelder));
        when(behandlingRevurderingRepository.finnKøetBehandlingMedforelder(fagsak)).thenReturn(Optional.of(køetBehandlingMedforelder));
    }

    private Behandling lagBehandling()
    {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        return scenario.lagMocked();
    }

    private Behandling lagRevurdering(Behandling behandling, BehandlingÅrsakType årsakType) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medOriginalBehandling(behandling, årsakType).medBehandlingType(BehandlingType.REVURDERING);
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        scenario.medFagsakId(behandling.getId()).medSaksnummer(behandling.getFagsak().getSaksnummer());
        return scenario.lagMocked();
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(Behandling behandling, BehandlingResultatType behandlingResultatType,
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
    public void testAvslutt1GMedKøBeggeSkalBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppKøBruker();
        settOppKø2Part();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(true);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - TODO: hva er riktig oppførsel? Nå blir køet behandling hos medforelder fortsatt og det opprettes ikke berørt
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testAvslutt1GMedKø2PartSkalBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppKø2Part();

        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(true);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - oppretter berørt behandling på medforelder
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
        verifyNoMoreInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    public void testAvslutt1GMedKøBrukerSkalBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppKøBruker();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(true);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert  - TODO: hva er riktig oppførsel? Nå fortsettes behandling i egen kø  ... opprettes ikke berørt
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandling, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testAvslutt1GMedKøIngenSkalBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(true);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert opprett berørt (for medforelder)
        verify(behandlingsoppretter).opprettRevurdering(fagsakMedforelder, BehandlingÅrsakType.BERØRT_BEHANDLING);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
    }

    @Test
    public void testAvslutt1GMedKøBeggeSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppKøBruker();
        settOppKø2Part();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - dekø fra medforelders kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testAvslutt1GMedKø2PartSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppKø2Part();

        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert  - dekø fra medforelders kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testAvslutt1GMedKøBrukerSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppKøBruker();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert dekø fra egen kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandling, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testAvslutt1GMedKøIngenSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(fBehandling.getId());
        // Assert - skal ikke skje noe
        verifyZeroInteractions(behandlingsoppretter);
        verifyZeroInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    public void testAvsluttBBMedKøBeggeSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppAvsluttetBerørt();
        settOppKøBruker();
        settOppKø2Part();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(berørt.getId());
        // Assert - dekø fra medforelders kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testAvsluttBBMedKø2PartSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppAvsluttetBerørt();
        settOppKø2Part();

        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(berørt.getId());
        // Assert   dekø fra medforelders kø
        verifyZeroInteractions(behandlingsoppretter);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandlingSettUtført(køetBehandlingMedforelder, Optional.of(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING));
    }

    @Test
    public void testAvsluttBBMedKøBrukerSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppAvsluttetBerørt();
        settOppKøBruker();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(berørt.getId());
        // Assert dekø fra egen kø og oppdater revurdering - bør kanskje begrenses ved senere tilpasning av berørt (endringssøknad, passert steg X)
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(køetBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(behandlingProsesseringTjeneste).opprettTasksForStartBehandling(any());
        verifyNoMoreInteractions(behandlingProsesseringTjeneste);
    }

    @Test
    public void testAvsluttBBMedKøIngenSkalIkkeBerøre() {
        // Arrange
        settOppAvsluttetBehandlingBruker();
        settOppAvsluttetBehandling2part();
        settOppAvsluttetBerørt();
        when(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(any(), any(Long.class), any(Long.class))).thenReturn(false);
        // Act
        berørtBehandlingKontroller.vurderNesteOppgaveIBehandlingskø(berørt.getId());
        // Assert - skal ikke skje noe
        verifyZeroInteractions(behandlingsoppretter);
        verifyZeroInteractions(behandlingProsesseringTjeneste);
    }
}

