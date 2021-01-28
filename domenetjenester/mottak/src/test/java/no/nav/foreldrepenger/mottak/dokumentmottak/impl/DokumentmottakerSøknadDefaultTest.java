package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ExtendWith(MockitoExtension.class)
public class DokumentmottakerSøknadDefaultTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private Kompletthetskontroller kompletthetskontroller;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private KøKontroller køKontroller;

    private DokumentmottakerSøknad dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @BeforeEach
    public void oppsett() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        fagsakRelasjonRepository = new FagsakRelasjonRepository(entityManager, new YtelsesFordelingRepository(entityManager),
                new FagsakLåsRepository(entityManager));
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);

        BehandlendeEnhetTjeneste enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, prosessTaskRepository, enhetsTjeneste,
                historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter);
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        var fpUttakTjeneste = new ForeldrepengerUttakTjeneste(new FpUttakRepository(entityManager));
        dokumentmottaker = new DokumentmottakerSøknadDefault(repositoryProvider, dokumentmottakerFelles,
                behandlingsoppretter, kompletthetskontroller, køKontroller, fpUttakTjeneste);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    public void skal_starte_behandling_av_søknad() {
        // Arrange
        Fagsak fagsak = nyMorFødselFagsak();
        Long fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        Behandling behandlingMock = mock(Behandling.class);
        when(behandlingMock.getAktørId()).thenReturn(fagsak.getAktørId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(eq(fagsak), any(), any())).thenReturn(behandlingMock);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, null);

        // Assert
        verify(dokumentmottaker).håndterIngenTidligereBehandling(fagsak, mottattDokument, null);
        verify(behandlingsoppretter).opprettFørstegangsbehandling(eq(fagsak), any(), any());
    }

    @Test
    public void skal_starte_behandling_av_papirsøknad_uten_metadata() {
        // Arrange
        Fagsak fagsak = nyMorFødselFagsak();
        Long fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattPapirsøknad(DokumentTypeId.UDEFINERT, fagsakId, "", now(), true, null);

        Behandling behandlingMock = mock(Behandling.class);
        when(behandlingMock.getAktørId()).thenReturn(fagsak.getAktørId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(any(), any(), any())).thenReturn(behandlingMock);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, null);

        // Assert
        verify(dokumentmottaker).håndterIngenTidligereBehandling(fagsak, mottattDokument, null);
        verify(behandlingsoppretter).opprettFørstegangsbehandling(any(), any(), any());
        verify(dokumentmottakerFelles).opprettInitiellFørstegangsbehandling(fagsak, mottattDokument, null);
    }

    @Test
    public void skal_tilbake_til_steg_registrer_søknad_dersom_åpen_behandling() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad
                .forFødselUtenSøknad()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagre(repositoryProvider);

        Long fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        String xml = null; // papirsøknad
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, xml, now(), false, null);

        when(kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)).thenReturn(true);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, null);
        verify(kompletthetskontroller).flyttTilbakeTilRegistreringPapirsøknad(behandling);
    }

    @Test
    public void skal_opprette_task_dersom_papirsøknad_ved_åpen_behandling_og_behandlingstype_ikke_støtter_søknadssteg() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad
                .forFødselUtenSøknad()
                .medBehandlingType(BehandlingType.REVURDERING)
                .lagre(repositoryProvider);

        Long fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        String xml = null; // papirsøknad
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, xml, now(), false, null);
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        when(kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)).thenReturn(false);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
    }

    @Test
    public void skal_lage_revurdering_når_det_finnes_en_avsluttet_behandling_på_saken_fra_før() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.AVSLAG);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        Behandling revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getAktørId()).thenReturn(behandling.getAktørId());
        doReturn(revurdering).when(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottaker).håndterAvsluttetTidligereBehandling(mottattDokument, behandling.getFagsak(), null);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettRevurdering(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    public void skal_opprette_køet_revurdering_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_siste_behandling_var_innvilget() {
        // Arrange - opprette innvilget behandling
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        Fagsak fagsak = behandling.getFagsak();

        // Arrange - mock tjenestekall
        Behandling nyBehandling = mock(Behandling.class);
        when(behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(nyBehandling);

        // Act - send inn søknad
        Long fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettKøetRevurdering(mottattDokument, fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    public void skal_opprette_køet_førstegangsbehandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_siste_behandling_var_avslått() {
        // Arrange - opprette avslått behandling
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.AVSLAG);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        Fagsak fagsak = behandling.getFagsak();

        // Arrange - mock tjenestekall
        Behandling nyBehandling = mock(Behandling.class);
        when(behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(eq(fagsak), any(), any(), anyBoolean()))
                .thenReturn(nyBehandling);
        when(behandlingsoppretter.erAvslåttBehandling(behandling)).thenReturn(Boolean.TRUE);

        // Act - send inn søknad
        Long fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER,
                behandling, false);
        verify(behandlingsoppretter).settSomKøet(nyBehandling);
    }

    @Test
    public void skal_opprette_køet_førstegangsbehandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_ingen_tidligere_behandling_finnes() {
        // Arrange - opprette fagsak uten behandling
        Fagsak fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, new Saksnummer("123"),
                fagsakRepository, fagsakRelasjonRepository);

        // Arrange - mock tjenestekall
        Behandling nyBehandling = mock(Behandling.class);

        // Act - send inn søknad
        Long fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        when(behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(eq(fagsak), any(), any(), anyBoolean()))
                .thenReturn(nyBehandling);

        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, null,
                false);
        verify(behandlingsoppretter).settSomKøet(nyBehandling);
    }

    @Test
    public void skal_oppdatere_køet_behandling_og_kjøre_kompletthet_dersom_køet_behandling_finnes() {
        // Arrange - opprette køet førstegangsbehandling
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Act - send inn søknad
        Long fagsakId = behandling.getFagsakId();
        Fagsak fagsak = behandling.getFagsak();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, null);

        // Assert - verifiser flyt
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    public void skal_henlegge_køet_behandling_dersom_søknad_mottatt_tidligere() {
        // Arrange - opprette køet førstegangsbehandling
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Arrange - legg inn søknad i mottatte dokumenter
        when(mottatteDokumentTjeneste.harMottattDokumentSet(any(), anySet())).thenReturn(true);

        // Arrange - mock tjenestekall
        Behandling nyKøetBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))
                .thenReturn(nyKøetBehandling);

        // Arrange - bygg søknad
        Long fagsakId = behandling.getFagsakId();
        Fagsak fagsak = behandling.getFagsak();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        // Act
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, null);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    public void skal_lage_ny_førstegangsbehandling_når_det_finnes_en_henlagt_førstegangsbehandling_på_saken_fra_før() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(
                        Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn(true).when(behandlingsoppretter).erBehandlingOgFørstegangsbehandlingHenlagt(behandling.getFagsak());
        doReturn(behandling).when(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(eq(behandling.getFagsak()), any(),
                any(), anyBoolean());

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottaker).håndterAvsluttetTidligereBehandling(mottattDokument, behandling.getFagsak(), null);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(StartBehandlingTask.TASKTYPE);
    }

    @Test
    public void skal_lage_ny_førstegangsbehandling_fra_henlagt_førstegangsbehandling_på_saken_fra_før() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        doReturn(behandling).when(behandlingsoppretter).opprettNyFørstegangsbehandlingFraTidligereSøknad(behandling.getFagsak(),
                BehandlingÅrsakType.ETTER_KLAGE, behandling);

        // Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument,
                BehandlingÅrsakType.ETTER_KLAGE, false);

        // Assert
        verify(dokumentmottaker).opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument,
                BehandlingÅrsakType.ETTER_KLAGE, false);
        verify(behandlingsoppretter).opprettNyFørstegangsbehandlingFraTidligereSøknad(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE,
                behandling);
    }

    @Test
    public void skal_lage_manuell_revurdering_fra_opphørt_førstegangsbehandling_på_saken_fra_før() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.OPPHØR));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.OPPHØR);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        doReturn(behandling).when(behandlingsoppretter).opprettManuellRevurdering(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE);

        // Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument,
                BehandlingÅrsakType.ETTER_KLAGE, false);

        // Assert
        verify(dokumentmottaker).opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument,
                BehandlingÅrsakType.ETTER_KLAGE, false);
        verify(behandlingsoppretter).opprettManuellRevurdering(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE);
    }

    @Test
    public void skal_finne_at_søknad_fra_tidligere_behandling_er_mottatt_og_henlegge_åpen_behandling() {
        // Arrange
        Behandling behandling1 = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        Fagsak fagsak = behandling1.getFagsak();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(true);

        Behandling behandling3 = Behandling.fraTidligereBehandling(behandling2, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker
        // scenarioet, og mock(Behandling) er heller ikke pent...
        // setInternalState(behandling3, "id", 9999L);
        behandling3.setId(9999L);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(behandling3);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), null);

        // Assert
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument);

        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(StartBehandlingTask.TASKTYPE);
    }

    @Test
    public void skal_henlegge_åpen_behandling_og_putte_ny_kø_hvis_medforelder_har_køet_revurdering() {
        // Arrange
        Behandling behandling1 = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .medDefaultFordeling(LocalDate.now())
                .medDefaultOppgittDekningsgrad()
                .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        Fagsak fagsak = behandling1.getFagsak();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(true);

        Behandling behandling3 = Behandling.fraTidligereBehandling(behandling2, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker
        // scenarioet, og mock(Behandling) er heller ikke pent...
        // setInternalState(behandling3, "id", 9999L);
        behandling3.setId(9999L);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(behandling3);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);

        Behandling behandlingMedforelder = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .lagre(repositoryProvider);
        simulerKøetBehandling(behandlingMedforelder);
        kobleFagsaker(behandling1, behandlingMedforelder);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), null);

        // Assert
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument);

        verify(køKontroller).dekøFørsteBehandlingISakskompleks(behandling3);
    }

    @Test
    public void skal_finne_at_søknad_ikke_er_mottatt_tidligere_og_knytte_søknaden_til_behandlingen() {
        // Arrange
        Behandling behandling1 = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        Fagsak fagsak = behandling1.getFagsak();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(false);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), null);

        // Assert
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument);
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling2, mottattDokument);
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    private Fagsak nyMorFødselFagsak() {
        return ScenarioMorSøkerEngangsstønad.forFødselUtenSøknad().lagreFagsak(repositoryProvider);
    }

    private void simulerKøetBehandling(Behandling behandling) {
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING);
    }

    private void kobleFagsaker(Behandling behandling, Behandling medforeldersBehandling) {
        fagsakRelasjonRepository.kobleFagsaker(behandling.getFagsak(), medforeldersBehandling.getFagsak(), behandling);
    }
}
