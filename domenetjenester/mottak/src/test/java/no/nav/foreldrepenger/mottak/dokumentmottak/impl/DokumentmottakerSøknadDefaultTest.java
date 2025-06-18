package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class DokumentmottakerSøknadDefaultTest extends EntityManagerAwareTest {

    private static final OrganisasjonsEnhet ENHET = new OrganisasjonsEnhet("4833", "NFP");

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRepository fagsakRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
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
    @Mock
    private ForeldrepengerUttakTjeneste fpUttakTjeneste;

    private DokumentmottakerSøknad dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @BeforeEach
    public void oppsett() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();

        var enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);
        lenient().when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(ENHET);
        lenient().when(enhetsTjeneste.finnBehandlendeEnhetFor(any(), any(String.class))).thenReturn(ENHET);
        lenient().when(enhetsTjeneste.finnBehandlendeEnhetFra(any())).thenReturn(ENHET);

        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var behandlingRevurderingTjeneste = new BehandlingRevurderingTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, behandlingRevurderingTjeneste, taskTjeneste, enhetsTjeneste,
                historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter, mock(TomtUttakTjeneste.class));
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        dokumentmottaker = new DokumentmottakerSøknadDefault(repositoryProvider.getBehandlingRepository(), dokumentmottakerFelles,
                behandlingsoppretter, kompletthetskontroller, køKontroller, fpUttakTjeneste, behandlingRevurderingTjeneste);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    void skal_starte_behandling_av_søknad() {
        // Arrange
        var fagsak = nyMorFødselFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        var behandlingMock = mock(Behandling.class);
        when(behandlingMock.getSaksnummer()).thenReturn(fagsak.getSaksnummer());
        when(behandlingsoppretter.opprettFørstegangsbehandling(eq(fagsak), any(), any())).thenReturn(behandlingMock);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, null);

        // Assert
        verify(dokumentmottaker).håndterIngenTidligereBehandling(fagsak, mottattDokument, null);
        verify(behandlingsoppretter).opprettFørstegangsbehandling(eq(fagsak), any(), any());
    }

    @Test
    void skal_starte_behandling_av_papirsøknad_uten_metadata() {
        // Arrange
        var fagsak = nyMorFødselFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattPapirsøknad(DokumentTypeId.UDEFINERT, fagsakId, "", now(), true, null);

        var behandlingMock = mock(Behandling.class);
        when(behandlingMock.getSaksnummer()).thenReturn(fagsak.getSaksnummer());
        when(behandlingsoppretter.opprettFørstegangsbehandling(any(), any(), any())).thenReturn(behandlingMock);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, null);

        // Assert
        verify(dokumentmottaker).håndterIngenTidligereBehandling(fagsak, mottattDokument, null);
        verify(behandlingsoppretter).opprettFørstegangsbehandling(any(), any(), any());
        verify(dokumentmottakerFelles).opprettInitiellFørstegangsbehandling(fagsak, mottattDokument, null);
    }

    @Test
    void skal_tilbake_til_steg_registrer_søknad_dersom_åpen_behandling() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad
                .forFødselUtenSøknad()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagre(repositoryProvider);

        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        String xml = null; // papirsøknad
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, xml, now(), false, null);

        when(kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)).thenReturn(true);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, null);
        verify(kompletthetskontroller).flyttTilbakeTilRegistreringPapirsøknad(eq(behandling), any());
    }

    @Test
    void skal_opprette_task_dersom_papirsøknad_ved_åpen_behandling_og_behandlingstype_ikke_støtter_søknadssteg() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad
                .forFødselUtenSøknad()
                .medBehandlingType(BehandlingType.REVURDERING)
                .lagre(repositoryProvider);

        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        String xml = null; // papirsøknad
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, xml, now(), false, null);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        when(kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)).thenReturn(false);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(OpprettOppgaveVurderDokumentTask.class));
    }

    @Test
    void skal_lage_revurdering_når_det_finnes_en_avsluttet_behandling_på_saken_fra_før() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.AVSLAG);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        var revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getSaksnummer()).thenReturn(behandling.getSaksnummer());
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
    void skal_opprette_køet_revurdering_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_siste_behandling_var_innvilget() {
        // Arrange - opprette innvilget behandling
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        var fagsak = behandling.getFagsak();

        // Arrange - mock tjenestekall
        var nyBehandling = mock(Behandling.class);
        when(behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(nyBehandling);

        // Act - send inn søknad
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettKøetRevurdering(mottattDokument, fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    void skal_opprette_køet_førstegangsbehandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_siste_behandling_var_avslått() {
        // Arrange - opprette avslått behandling
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.AVSLAG);
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));
        var fagsak = behandling.getFagsak();

        // Arrange - mock tjenestekall
        var nyBehandling = mock(Behandling.class);
        doReturn(fagsak.getSaksnummer()).when(nyBehandling).getSaksnummer();
        when(behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(eq(fagsak), any(), any(), anyBoolean()))
                .thenReturn(nyBehandling);
        when(behandlingsoppretter.erAvslåttBehandling(behandling)).thenReturn(Boolean.TRUE);

        // Act - send inn søknad
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER,
                behandling, false);
        verify(taskTjeneste).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_opprette_køet_førstegangsbehandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_ingen_tidligere_behandling_finnes() {
        // Arrange - opprette fagsak uten behandling
        var aktørId = AktørId.dummy();
        var saksnummer = new Saksnummer("9999");
        var fagsak = DokumentmottakTestUtil.byggFagsak(aktørId, RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, saksnummer,
                fagsakRepository);

        // Arrange - mock tjenestekall
        var nyBehandling = mock(Behandling.class);
        doReturn(saksnummer).when(nyBehandling).getSaksnummer();

        // Act - send inn søknad
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        when(behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(eq(fagsak), any(), any(), anyBoolean()))
                .thenReturn(nyBehandling);

        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, null,
                false);
        verify(taskTjeneste).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_oppdatere_køet_behandling_og_kjøre_kompletthet_dersom_køet_behandling_finnes() {
        // Arrange - opprette køet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Act - send inn søknad
        var fagsakId = behandling.getFagsakId();
        var fagsak = behandling.getFagsak();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, null);

        // Assert - verifiser flyt
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    void skal_henlegge_køet_behandling_dersom_søknad_mottatt_tidligere() {
        // Arrange - opprette køet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Arrange - legg inn søknad i mottatte dokumenter
        when(mottatteDokumentTjeneste.harMottattDokumentSet(any(), anySet())).thenReturn(true);

        // Arrange - mock tjenestekall
        var nyKøetBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))
                .thenReturn(nyKøetBehandling);

        // Arrange - bygg søknad
        var fagsakId = behandling.getFagsakId();
        var fagsak = behandling.getFagsak();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        // Act
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, null);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    void skal_lage_ny_førstegangsbehandling_når_det_finnes_en_henlagt_førstegangsbehandling_på_saken_fra_før() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingsresultat(
                        Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET))
                .lagre(repositoryProvider);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, repositoryProvider.getBehandlingRepository().taSkriveLås(behandling));

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn(true).when(behandlingsoppretter).erBehandlingOgFørstegangsbehandlingHenlagt(behandling.getFagsak());
        doReturn(behandling).when(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(eq(behandling.getFagsak()), any(),
                any(), anyBoolean());

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottaker).håndterAvsluttetTidligereBehandling(mottattDokument, behandling.getFagsak(), null);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(StartBehandlingTask.class));
    }

    @Test
    void skal_lage_ny_førstegangsbehandling_fra_henlagt_førstegangsbehandling_på_saken_fra_før() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET));
        var behandling = scenario.lagre(repositoryProvider);

        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
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
    void skal_lage_ny_førstegangsbehandling_når_opphørt_ingen_perioder() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.OPPHØR))
            .lagre(repositoryProvider);

        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.OPPHØR);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());
        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn(true).when(behandlingsoppretter).erOpphørtBehandling(behandling);
        doReturn(behandling).when(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(eq(behandling.getFagsak()), any(),
            any(), anyBoolean());

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(),  behandling, null);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(StartBehandlingTask.class));
    }

    @Test
    void skal_lage_revurdering_når_opphørt_med_innvilget_perioder() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.OPPHØR))
            .lagre(repositoryProvider);

        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.OPPHØR);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());
        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        var uttakPeriode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(LocalDate.now(), LocalDate.now()).medResultatType(PeriodeResultatType.INNVILGET).build();
        doReturn(true).when(behandlingsoppretter).erOpphørtBehandling(behandling);
        doReturn(Optional.of(new ForeldrepengerUttak(List.of(uttakPeriode)))).when(fpUttakTjeneste).hentHvisEksisterer(behandling.getId());
        doReturn(behandling).when(behandlingsoppretter).opprettRevurdering(eq(behandling.getFagsak()), any());

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(),  behandling, null);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(StartBehandlingTask.class));
    }

    @Test
    void skal_lage_manuell_revurdering_fra_opphørt_førstegangsbehandling_på_saken_fra_før() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.OPPHØR));
        var behandling = scenario.lagre(repositoryProvider);

        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.OPPHØR);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());

        // simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
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
    void skal_finne_at_søknad_fra_tidligere_behandling_er_mottatt_og_henlegge_åpen_behandling() {
        // Arrange
        var behandling1 = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        var fagsak = behandling1.getFagsak();

        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(true);

        var behandling3 = Behandling.fraTidligereBehandling(behandling2, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker
        // scenarioet, og mock(Behandling) er heller ikke pent...
        // setInternalState(behandling3, "id", 9999L);
        behandling3.setId(9999L);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(behandling3);

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), null);

        // Assert
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(any(), eq(mottattDokument));

        verify(køKontroller).dekøFørsteBehandlingISakskompleks(behandling3);
    }

    @Test
    void skal_henlegge_åpen_behandling_og_putte_ny_kø_hvis_medforelder_har_køet_revurdering() {
        // Arrange
        var behandling1 = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .medDefaultFordeling(LocalDate.now())
                .medDefaultOppgittDekningsgrad()
                .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        var fagsak = behandling1.getFagsak();

        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(true);

        var behandling3 = Behandling.fraTidligereBehandling(behandling2, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker
        // scenarioet, og mock(Behandling) er heller ikke pent...
        // setInternalState(behandling3, "id", 9999L);
        behandling3.setId(9999L);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(behandling3);

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);

        var behandlingMedforelder = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .lagre(repositoryProvider);
        simulerKøetBehandling(behandlingMedforelder);
        fagsakRelasjonTjeneste.kobleFagsaker(behandling1.getFagsak(), behandlingMedforelder.getFagsak());

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), null);

        // Assert
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(any(), eq(mottattDokument));

        verify(køKontroller).dekøFørsteBehandlingISakskompleks(behandling3);
    }

    @Test
    void skal_finne_at_søknad_ikke_er_mottatt_tidligere_og_knytte_søknaden_til_behandlingen() {
        // Arrange
        var behandling1 = ScenarioMorSøkerForeldrepenger
                .forFødsel()
                .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        var fagsak = behandling1.getFagsak();

        var behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(false);

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), null);

        // Assert
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument);
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling2, mottattDokument);
    }

    private void lagreBehandling(Behandling behandling) {
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
    }

    private Fagsak nyMorFødselFagsak() {
        return ScenarioMorSøkerEngangsstønad.forFødselUtenSøknad().lagreFagsak(repositoryProvider);
    }

    private void simulerKøetBehandling(Behandling behandling) {
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING);
    }

}
