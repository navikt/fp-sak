package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static no.nav.vedtak.felles.testutilities.Whitebox.setInternalState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
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
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class DokumentmottakerSøknadDefaultTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    @Inject
    private ForeldrepengerUttakTjeneste fpUttakTjeneste;

    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();

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

    @Before
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        BehandlendeEnhetTjeneste enhetsTjeneste = mock(BehandlendeEnhetTjeneste.class);
        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(enhetsTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, prosessTaskRepository, enhetsTjeneste,
            historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter);
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        dokumentmottaker = new DokumentmottakerSøknadDefault(repositoryProvider, dokumentmottakerFelles, mottatteDokumentTjeneste,
            behandlingsoppretter, kompletthetskontroller, køKontroller, fpUttakTjeneste);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    public void skal_starte_behandling_av_søknad() {
        //Arrange
        Fagsak fagsak = nyMorFødselFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        Behandling behandlingMock = mock(Behandling.class);
        when(behandlingMock.getAktørId()).thenReturn(fagsak.getAktørId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(eq(fagsak), any(), any())).thenReturn(behandlingMock);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).håndterIngenTidligereBehandling(fagsak, mottattDokument, null);
        verify(behandlingsoppretter).opprettFørstegangsbehandling(eq(fagsak), any(), any());
        verify(dokumentmottakerFelles).opprettHistorikk(any(Behandling.class), eq(mottattDokument.getJournalpostId()));
    }

    @Test
    public void skal_starte_behandling_av_papirsøknad_uten_metadata() {
        //Arrange
        Fagsak fagsak = nyMorFødselFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattPapirsøknad(DokumentTypeId.UDEFINERT, fagsakId, "", now(), true, null);

        Behandling behandlingMock = mock(Behandling.class);
        when(behandlingMock.getAktørId()).thenReturn(fagsak.getAktørId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(any(), any(), any())).thenReturn(behandlingMock);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).håndterIngenTidligereBehandling(fagsak, mottattDokument, null);
        verify(behandlingsoppretter).opprettFørstegangsbehandling(any(), any(), any());
        verify(dokumentmottakerFelles).opprettHistorikk(any(Behandling.class), eq(mottattDokument.getJournalpostId()));
    }

    @Test
    public void skal_tilbake_til_steg_registrer_søknad_dersom_åpen_behandling() {
        //Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);

        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        String xml = null; // papirsøknad
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, xml, now(), false, null);

        when(kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)).thenReturn(true);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, null);
        verify(kompletthetskontroller).flyttTilbakeTilRegistreringPapirsøknad(behandling);
    }

    @Test
    public void skal_opprette_task_dersom_papirsøknad_ved_åpen_behandling_og_behandlingstype_ikke_støtter_søknadssteg() {
        //Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);

        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        String xml = null; // papirsøknad
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, xml, now(), false, null);
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        when(kompletthetskontroller.støtterBehandlingstypePapirsøknad(behandling)).thenReturn(false);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument.getJournalpostId());

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
    }

    @Test
    public void skal_lage_revurdering_når_det_finnes_en_avsluttet_behandling_på_saken_fra_før() {
        //Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.AVSLAG);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        //simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        Behandling revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getFagsak()).thenReturn(behandling.getFagsak());
        when(revurdering.getAktørId()).thenReturn(behandling.getAktørId());
        doReturn(revurdering).when(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).håndterAvsluttetTidligereBehandling(mottattDokument, behandling.getFagsak(), null);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(revurdering, mottattDokument.getJournalpostId());
    }

    @Test
    public void skal_opprette_køet_revurdering_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_siste_behandling_var_innvilget() {
        // Arrange - opprette innvilget behandling
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());
        Fagsak fagsak = behandling.getFagsak();

        // Arrange - mock tjenestekall
        Behandling nyBehandling = mock(Behandling.class);
        long behandlingId = 1L;
        doReturn(behandlingId).when(nyBehandling).getId();
        doReturn(fagsak).when(nyBehandling).getFagsak();
        when(behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(nyBehandling);

        // Act - send inn søknad
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(nyBehandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(nyBehandling, mottattDokument.getJournalpostId());
    }

    @Test
    public void skal_opprette_køet_førstegangsbehandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_siste_behandling_var_avslått() {
        // Arrange - opprette avslått behandling
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT));
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());
        Fagsak fagsak = behandling.getFagsak();

        // Arrange - mock tjenestekall
        Behandling nyBehandling = mock(Behandling.class);
        long behandlingId = 1L;
        doReturn(behandlingId).when(nyBehandling).getId();
        doReturn(fagsak).when(nyBehandling).getFagsak();
        when(behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty())).thenReturn(nyBehandling);

        // Act - send inn søknad
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        verify(behandlingsoppretter).settSomKøet(nyBehandling);
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(nyBehandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(nyBehandling, mottattDokument.getJournalpostId());
    }

    @Test
    public void skal_opprette_køet_førstegangsbehandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes_og_ingen_tidligere_behandling_finnes() {
        // Arrange - opprette fagsak uten behandling
        Fagsak fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, new Saksnummer("123"), fagsakRepository, fagsakRelasjonRepository);

        // Arrange - mock tjenestekall
        Behandling nyBehandling = mock(Behandling.class);
        long behandlingId = 1L;
        doReturn(behandlingId).when(nyBehandling).getId();
        doReturn(fagsak).when(nyBehandling).getFagsak();
        when(behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty())).thenReturn(nyBehandling);

        // Act - send inn søknad
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        verify(behandlingsoppretter).settSomKøet(nyBehandling);
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(nyBehandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(nyBehandling, mottattDokument.getJournalpostId());
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
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, null);

        // Assert - verifiser flyt
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument.getJournalpostId());
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
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        // Act
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL, null);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(nyKøetBehandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument.getJournalpostId());
    }

    @Test
    public void skal_lage_ny_førstegangsbehandling_når_det_finnes_en_henlagt_førstegangsbehandling_på_saken_fra_før() {
        //Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        //simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        doReturn(true).when(behandlingsoppretter).erBehandlingOgFørstegangsbehandlingHenlagt(behandling.getFagsak());
        doReturn(behandling).when(behandlingsoppretter).opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(null, behandling.getFagsak());

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), dokumentTypeId, null);

        //Assert
        verify(dokumentmottaker).håndterAvsluttetTidligereBehandling(mottattDokument, behandling.getFagsak(), null);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(StartBehandlingTask.TASKTYPE);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument.getJournalpostId());
    }

    @Test
    public void skal_lage_ny_førstegangsbehandling_fra_henlagt_førstegangsbehandling_på_saken_fra_før() {
        //Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.UDEFINERT);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        //simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        doReturn(true).when(behandlingsoppretter).erBehandlingOgFørstegangsbehandlingHenlagt(behandling.getFagsak());
        doReturn(behandling).when(behandlingsoppretter).opprettNyFørstegangsbehandlingFraTidligereSøknad(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE, behandling);

        //Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument, BehandlingÅrsakType.ETTER_KLAGE, false);

        //Assert
        verify(dokumentmottaker).opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument, BehandlingÅrsakType.ETTER_KLAGE, false);
        verify(behandlingsoppretter).opprettNyFørstegangsbehandlingFraTidligereSøknad(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE, behandling);
    }

    @Test
    public void skal_lage_manuell_revurdering_fra_opphørt_førstegangsbehandling_på_saken_fra_før() {
        //Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.OPPHØR));
        Behandling behandling = scenario.lagre(repositoryProvider);

        BehandlingVedtak vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.OPPHØR);
        repoRule.getRepository().lagre(vedtak.getBehandlingsresultat());

        //simulere at den tidliggere behandligen er avsluttet
        behandling.avsluttBehandling();
        Long fagsakId = behandling.getFagsakId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        doReturn(false).when(behandlingsoppretter).erBehandlingOgFørstegangsbehandlingHenlagt(behandling.getFagsak());
        doReturn(behandling).when(behandlingsoppretter).opprettManuellRevurdering(behandling.getFagsak(), BehandlingÅrsakType.ETTER_KLAGE);

        //Act
        dokumentmottaker.opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument, BehandlingÅrsakType.ETTER_KLAGE, false);

        //Assert
        verify(dokumentmottaker).opprettFraTidligereAvsluttetBehandling(behandling.getFagsak(), behandling.getId(), mottattDokument, BehandlingÅrsakType.ETTER_KLAGE, false);
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

        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(true);

        Behandling behandling3 = Behandling.fraTidligereBehandling(behandling2, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker scenarioet, og mock(Behandling) er heller ikke pent...
        setInternalState(behandling3, "id", 9999L);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(behandling3);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);
        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), dokumentTypeId, null);

        // Assert
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument.getJournalpostId());

        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(StartBehandlingTask.TASKTYPE);
    }

    @Test
    public void skal_henlegge_åpen_behandling_og_putte_ny_kø_hvis_medforelder_har_køet_revurdering() {
        // Arrange
        Behandling behandling1 = ScenarioMorSøkerForeldrepenger
            .forFødsel()
            .medDefaultOppgittFordeling(LocalDate.now())
            .medDefaultOppgittDekningsgrad()
            .lagre(repositoryProvider);
        behandling1.avsluttBehandling();
        Fagsak fagsak = behandling1.getFagsak();

        Behandling behandling2 = Behandling.fraTidligereBehandling(behandling1, BehandlingType.FØRSTEGANGSSØKNAD).build();
        lagreBehandling(behandling2);

        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(true);

        Behandling behandling3 = Behandling.fraTidligereBehandling(behandling2, BehandlingType.FØRSTEGANGSSØKNAD).build();
        // Hack, men det blir feil å lagre Behandlingen før Act da det påvirker scenarioet, og mock(Behandling) er heller ikke pent...
        setInternalState(behandling3, "id", 9999L);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(behandling3);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);

        Behandling behandlingMedforelder = ScenarioMorSøkerForeldrepenger
            .forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);
        simulerKøetBehandling(behandlingMedforelder);
        kobleFagsaker(behandling1, behandlingMedforelder);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), dokumentTypeId, null);

        // Assert
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling2, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument.getJournalpostId());

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

        DokumentTypeId dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling1.getId()), anySet())).thenReturn(true);
        when(mottatteDokumentTjeneste.harMottattDokumentSet(eq(behandling2.getId()), anySet())).thenReturn(false);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsak.getId(), "<søknad>", now(), true, null);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling2.getFagsak(), dokumentTypeId, null);

        // Assert
        verify(dokumentmottakerFelles).opprettHistorikk(behandling2, mottattDokument.getJournalpostId());
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
        BehandlingÅrsakType berørtType = BehandlingÅrsakType.KØET_BEHANDLING;
        new BehandlingÅrsak.Builder(List.of(berørtType)).buildFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING);
    }

    private void kobleFagsaker(Behandling behandling, Behandling medforeldersBehandling) {
        fagsakRelasjonRepository.kobleFagsaker(behandling.getFagsak(), medforeldersBehandling.getFagsak(), behandling);
    }
}
