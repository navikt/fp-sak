package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class DokumentmottakerVedleggTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private Behandlingsoppretter behandlingsoppretter;

    @Inject
    private KlageRepository klageRepository;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private DokumentmottakerVedlegg dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private Kompletthetskontroller kompletthetskontroller;

    @Before
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, prosessTaskRepository, behandlendeEnhetTjeneste,
            historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter);
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        kompletthetskontroller = mock(Kompletthetskontroller.class);
        dokumentmottaker = new DokumentmottakerVedlegg(repositoryProvider, dokumentmottakerFelles, kompletthetskontroller);
        dokumentmottaker = Mockito.spy(dokumentmottaker);
        when(behandlendeEnhetTjeneste.getKlageInstans()).thenReturn(new OrganisasjonsEnhet("4292", "NAV Klageinstans Midt-Norge"));
    }

    @Test
    public void skal_opprette_task_for_å_vurdere_dokument_når_det_ikke_er_en_søknad_eller_har_en_behandling() {
        //Arrange
        Fagsak fagsak = nyMorFødselFagsak();
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        String behandlendeEnhet = dokumentmottakerFelles.hentBehandlendeEnhetTilVurderDokumentOppgave(mottattDokument, fagsak, null);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, null);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(null, mottattDokument);
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo(behandlendeEnhet);
    }

    @Test
    public void skal_vurdere_kompletthet_når_ustrukturert_dokument_på_åpen_behandling() {
        //Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP);
        Behandling behandling = scenario.lagre(repositoryProvider);

        DokumentTypeId dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, null);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        //Assert
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForVedlegg(behandling.getFagsakId(), mottattDokument.getJournalpostId(), dokumentTypeId);
    }

    @Test
    public void skal_opprette_task_for_å_vurdere_dokument_når_det_ikke_er_en_søknad_men_har_behandling_på_saken_og_komplett() {
        //Arrange
        DokumentTypeId dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlendeEnhet("0450")
            .medBehandlingStegStart(BehandlingStegType.FORESLÅ_VEDTAK);
        Behandling behandling = scenario.lagre(repositoryProvider);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, null);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo("0450"); //Lik enheten som ble satt på behandlingen
    }

    @Test
    public void skal_opprette_task_for_å_vurdere_dokument_når_klageinstans_har_sendt_brev_til_scanning() {
        //Arrange
        DokumentTypeId dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlendeEnhet("0450")
            .medBehandlingStegStart(BehandlingStegType.FORESLÅ_VEDTAK);
        Behandling behandling = scenario.lagre(repositoryProvider);

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, null);
        mottattDokument.setJournalEnhet(behandlendeEnhetTjeneste.getKlageInstans().getEnhetId());
        when(behandlendeEnhetTjeneste.gyldigEnhetNfpNk(any())).thenReturn(true);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo(behandlendeEnhetTjeneste.getKlageInstans().getEnhetId()); //Lik enheten som ble satt på behandlingen
    }

    /**
     * Vurder dokument oppgaver skal ikke bruke behandlende enhetsid fra klager.
     * Er behandlingen en klage skal vi hente ut behandlende enhet fra siste behandling som ikke er klage
     */
    @Test
    public void skal_opprette_task_for_å_vurdere_dokument_når_det_ikke_er_en_søknad_men_har_behandling_på_saken_hent_behandlende_enhet_fra_ikke_klagebehandling() {
        final String førstegangssøknadEnhetsId = "0450";
        final String klageEnhetsId = "5000";

        //Arrange
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(
            ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlendeEnhet(førstegangssøknadEnhetsId))
            .medBehandlendeEnhet(klageEnhetsId);
        Behandling klageBehandling = scenario.lagre(repositoryProvider, klageRepository);

        Long fagsakId = scenario.getFagsak().getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, klageBehandling.getFagsak(), null);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(klageBehandling.getFagsak(), klageBehandling, mottattDokument);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(klageBehandling, mottattDokument);
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo(førstegangssøknadEnhetsId); //Lik enheten som ble satt på behandlingen
    }

    @Test
    public void skal_ikke_opprette_køet_behandling_når_ingen_tidligere_behandling() {
        // Arrange - opprette fagsak uten behandling
        Fagsak fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, new Saksnummer("123"), repositoryProvider.getFagsakRepository(), repositoryProvider.getFagsakRelasjonRepository());

        // Act - send inn endringssøknad
        Long fagsakId = fagsak.getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.ANNET;
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.RE_ANNET);

        // Assert - verifiser flyt
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(null, mottattDokument);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    public void skal_opprette_task_for_å_vurdere_dokument_når_dokumenttype_er_udefinert() {
        final String førstegangssøknadEnhetsId = "0450";
        final String klageEnhetsId = "5000";

        //Arrange
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(
            ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlendeEnhet(førstegangssøknadEnhetsId))
            .medBehandlendeEnhet(klageEnhetsId);
        Behandling klageBehandling = scenario.lagre(repositoryProvider, klageRepository);

        Long fagsakId = scenario.getFagsak().getId();
        DokumentTypeId dokumentTypeId = DokumentTypeId.UDEFINERT;

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        ArgumentCaptor<ProsessTaskData> captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        //Act
        dokumentmottaker.mottaDokument(mottattDokument, klageBehandling.getFagsak(), null);

        //Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(klageBehandling.getFagsak(), klageBehandling, mottattDokument);
        verifyZeroInteractions(mottatteDokumentTjeneste);

        //Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(klageBehandling, mottattDokument);
        verify(prosessTaskRepository).lagre(captor.capture());
        ProsessTaskData prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.getTaskType()).isEqualTo(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo(førstegangssøknadEnhetsId); //Lik enheten som ble satt på behandlingen
    }

    private Fagsak nyMorFødselFagsak() {
        return ScenarioMorSøkerEngangsstønad.forFødselUtenSøknad().lagreFagsak(repositoryProvider);
    }
}
