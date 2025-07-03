package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.sakskompleks.KøKontroller;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
class DokumentmottakerEndringssøknadTest extends EntityManagerAwareTest {

    private static final OrganisasjonsEnhet ENHET = new OrganisasjonsEnhet("4833", "NFP");

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste enhetsTjeneste;

    @Mock
    private KøKontroller køKontroller;
    @Mock
    private Kompletthetskontroller kompletthetskontroller;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    private DokumentmottakerEndringssøknad dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @BeforeEach
    public void oppsett() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = repositoryProvider.getFagsakRepository();
        var fpUttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
        behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();

        lenient().when(enhetsTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(ENHET);
        lenient().when(enhetsTjeneste.finnBehandlendeEnhetFor(any(), any(String.class))).thenReturn(ENHET);
        lenient().when(enhetsTjeneste.finnBehandlendeEnhetFra(any())).thenReturn(ENHET);

        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var behandlingRevurderingTjeneste = new BehandlingRevurderingTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, behandlingRevurderingTjeneste, taskTjeneste,
            enhetsTjeneste, historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter,
            mock(TomtUttakTjeneste.class));
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        dokumentmottaker = new DokumentmottakerEndringssøknad(repositoryProvider.getBehandlingRepository(), dokumentmottakerFelles,
                behandlingsoppretter, kompletthetskontroller, køKontroller, fpUttakTjeneste, behandlingRevurderingTjeneste);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    void skal_opprette_task_for_manuell_vurdering_av_endringssøknad_dersom_ingen_behandling_finnes_fra_før() {
        //Arrange
        var fagsak = nyMorFødselFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeEndringssøknad = DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeEndringssøknad, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    void skal_opprette_revurdering_for_endringssøknad_dersom_siste_behandling_er_avsluttet() {
        //Arrange
        var behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .lagre(repositoryProvider);
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());
        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var revurdering = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .lagre(repositoryProvider);
        when(behandlingsoppretter.opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(revurdering);

        var fagsakId = behandling.getFagsakId();
        var dokumentTypeEndringssøknad = DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeEndringssøknad, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);//Behandlingårsaktype blir aldri satt for mottatt dokument, så input er i udefinert.

        //Assert
        verify(behandlingsoppretter).opprettRevurdering(behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER); //Skal ved opprettelse av revurdering fra endringssøknad sette behandlingårsaktype til 'endring fra bruker'.
    }

    @Test
    void skal_oppdatere_behandling_med_endringssøknad_dersom_siste_behandling_er_åpen() {
        //Arrange
        var behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .lagre(repositoryProvider);
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());
        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var revurdering = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);

        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(revurdering, mottattDokument, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(revurdering, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(revurdering, mottattDokument);
    }

    @Test
    void skal_oppdatere_behandling_med_endringssøknad_dersom_siste_behandling_er_åpen_og_berørt_i_mellomtiden() {
        //Arrange
        var behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .lagre(repositoryProvider);
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());
        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var revurdering = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);

        var berørt = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medOriginalBehandling(behandling, BehandlingÅrsakType.BERØRT_BEHANDLING)
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);
        var vedtakBerørt = DokumentmottakTestUtil.oppdaterVedtaksresultat(berørt, VedtakResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(vedtakBerørt.getBehandlingsresultat().getBehandlingId(), vedtakBerørt.getBehandlingsresultat());
        // simulere at den tidligere behandlingen er avsluttet
        berørt.avsluttBehandling();
        behandlingRepository.lagre(berørt, behandlingRepository.taSkriveLås(berørt));


        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Assert - litt fake - skulle vært oppdatereViaHenleggelse (mottattDokumentTjeneste -> true)
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(revurdering, mottattDokument, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(revurdering, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikk(revurdering, mottattDokument);
    }

    @Test
    void skal_dekøe_første_behandling_i_sakskompleks_dersom_endringssøknad_på_endringssøknad() {
        // Arrange - opprette førstegangsbehandling
        var behandling = ScenarioMorSøkerForeldrepenger
            .forFødsel()
            .lagre(repositoryProvider);
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());

        var revurdering = ScenarioMorSøkerForeldrepenger
            .forFødsel()
            .medFagsakId(behandling.getFagsakId())
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);
        var behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);
        simulerKøetBehandling(revurdering);

        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();

        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        // Arrange - mock
        var nyBehandling = mock(Behandling.class);
        when(nyBehandling.getFagsak()).thenReturn(revurdering.getFagsak());
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(revurdering, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(nyBehandling);

        // Arrange - legg inn endringssøknad i mottatte dokumenter
        when(mottatteDokumentTjeneste.harMottattDokumentSet(revurdering.getId(), DokumentTypeId.getEndringSøknadTyper())).thenReturn(true);

        //Act - bygg søknad
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(any(Behandling.class), eq(mottattDokument), eq(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER));
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(revurdering, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(køKontroller).dekøFørsteBehandlingISakskompleks(nyBehandling);
    }

    @Test
    void skal_opprette_køet_behandling_og_kjøre_kompletthet_dersom_køet_behandling_ikke_finnes() {
        // Arrange - opprette fagsak uten behandling
        var gammelbehandling = ScenarioMorSøkerForeldrepenger
            .forFødsel()
            .medBehandlingsresultat(new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET))
            .lagre(repositoryProvider);
        gammelbehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(gammelbehandling, repositoryProvider.getBehandlingRepository().taSkriveLås(gammelbehandling));
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(gammelbehandling, VedtakResultatType.INNVILGET);
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, repositoryProvider.getBehandlingRepository().taSkriveLås(gammelbehandling));
        var fagsak = gammelbehandling.getFagsak();
        // Arrange - mock tjenestekall
        var behandling = mock(Behandling.class);
        doReturn(false).when(behandlingsoppretter).erBehandlingOgFørstegangsbehandlingHenlagt(fagsak);
        when(behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).thenReturn(behandling);

        // Act - send inn endringssøknad
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).opprettRevurdering(fagsak, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    void skal_ikke_opprette_køet_behandling_når_alle_henlagt() {
        // Arrange - opprette fagsak uten behandling
        var fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, new Saksnummer("9999"),
            fagsakRepository);

        // Arrange - mock tjenestekall
        when(behandlingsoppretter.erBehandlingOgFørstegangsbehandlingHenlagt(fagsak)).thenReturn(true);

        // Act - send inn endringssøknad
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    void skal_oppdatere_køet_behandling_og_kjøre_kompletthet_dersom_køet_behandling_finnes() {
        // Arrange - opprette køet behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.REVURDERING);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Act - send inn endringssøknad
        var fagsakId = behandling.getFagsakId();
        var fagsak = behandling.getFagsak();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(kompletthetskontroller).persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    void skal_henlegge_køet_behandling_dersom_endringssøknad_mottatt_tidligere() {
        // Arrange - opprette køet behandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medSøknadDato(LocalDate.now().minusDays(3))
            .medBehandlingType(BehandlingType.REVURDERING);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Arrange - mock tjenestekall
        var nyKøetBehandling = ScenarioFarSøkerForeldrepenger.forFødsel().lagre(repositoryProvider);
        when(behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER))
            .thenReturn(nyKøetBehandling);

        when(mottatteDokumentTjeneste.harMottattDokumentSet(behandling.getId(), DokumentTypeId.getEndringSøknadTyper())).thenReturn(true);

        // Arrange - bygg søknad
        var fagsakId = behandling.getFagsakId();
        var fagsak = behandling.getFagsak();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        // Act
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(behandlingsoppretter).oppdaterBehandlingViaHenleggelse(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettHistorikk(behandling, mottattDokument);
    }

    @Test
    void skal_oppdatere_mottatt_dokument_med_behandling_hvis_behandlig_er_på_vent() {
        //Arrange
        var behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .lagre(repositoryProvider);
        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, VedtakResultatType.INNVILGET);
        behandlingsresultatRepository.lagre(vedtak.getBehandlingsresultat().getBehandlingId(), vedtak.getBehandlingsresultat());
        // simulere at den tidligere behandlingen er avsluttet
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var revurdering = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .medFagsakId(behandling.getFagsakId())
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagre(repositoryProvider);

        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.UDEFINERT);

        //Assert
        verify(mottatteDokumentTjeneste).oppdaterMottattDokumentMedBehandling(mottattDokument, revurdering.getId());
    }

    @Test
    void skal_opprette_vurder_dokument_oppgave_i_gosys_dersom_det_er_åpen_førstegangsbehandling() {
        //Arrange
        var behandling = ScenarioMorSøkerEngangsstønad
            .forFødselUtenSøknad()
            .lagre(repositoryProvider);

        var fagsakId = behandling.getFagsakId();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);

        //Assert
        verify(dokumentmottaker).oppdaterÅpenBehandlingMedDokument(behandling, mottattDokument, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);
    }

    @Test
    void skal_opprette_vurder_dokument_oppgave_hvis_køet_førstegangsbehandling_mottar_endringssøknad() {
        // Arrange - opprette køet førstegangsbehandling
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandling = scenario.lagre(repositoryProvider);
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        simulerKøetBehandling(behandling);

        // Act - send inn endringssøknad
        var fagsakId = behandling.getFagsakId();
        var fagsak = behandling.getFagsak();
        var dokumentTypeId = DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.UDEFINERT);

        // Assert - verifiser flyt
        verify(kompletthetskontroller, times(0)).persisterKøetDokumentOgVurderKompletthet(behandling, mottattDokument, Optional.empty());
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, behandling, mottattDokument);
    }

    private void simulerKøetBehandling(Behandling behandling) {
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING);
    }

    private Fagsak nyMorFødselFagsak() {
        return ScenarioMorSøkerEngangsstønad.forFødselUtenSøknad().lagreFagsak(repositoryProvider);
    }
}
