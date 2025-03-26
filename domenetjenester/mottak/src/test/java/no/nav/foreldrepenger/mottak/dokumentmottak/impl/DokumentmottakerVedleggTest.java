package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@CdiDbAwareTest
class DokumentmottakerVedleggTest {

    private static final OrganisasjonsEnhet ENHET = new OrganisasjonsEnhet("4833", "NFP");

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;

    @Inject
    private Behandlingsoppretter behandlingsoppretter;

    @Inject
    private KlageRepository klageRepository;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private DokumentmottakerVedlegg dokumentmottaker;
    private DokumentmottakerFelles dokumentmottakerFelles;
    private Kompletthetskontroller kompletthetskontroller;

    @BeforeEach
    public void oppsett() {

        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any())).thenReturn(ENHET);
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(), any(String.class))).thenReturn(ENHET);
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(any())).thenReturn(ENHET);

        dokumentmottakerFelles = new DokumentmottakerFelles(repositoryProvider, behandlingRevurderingTjeneste, taskTjeneste, behandlendeEnhetTjeneste,
                historikkinnslagTjeneste, mottatteDokumentTjeneste, behandlingsoppretter, mock(TomtUttakTjeneste.class));
        dokumentmottakerFelles = Mockito.spy(dokumentmottakerFelles);

        kompletthetskontroller = mock(Kompletthetskontroller.class);
        dokumentmottaker = new DokumentmottakerVedlegg(behandlingRevurderingTjeneste, dokumentmottakerFelles, kompletthetskontroller, repositoryProvider.getBehandlingRepository());
        dokumentmottaker = Mockito.spy(dokumentmottaker);
    }

    @Test
    void skal_opprette_task_for_å_vurdere_dokument_når_det_ikke_er_en_søknad_eller_har_en_behandling() {
        // Arrange
        var fagsak = nyMorFødselFagsak();
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        var behandlendeEnhet = dokumentmottakerFelles.hentBehandlendeEnhetTilVurderDokumentOppgave(mottattDokument, fagsak, null);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, null);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(null, mottattDokument);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(OpprettOppgaveVurderDokumentTask.class));
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo(behandlendeEnhet);
    }

    @Test
    void skal_vurdere_kompletthet_når_ustrukturert_dokument_på_åpen_behandling() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP);
        var behandling = scenario.lagre(repositoryProvider);

        var dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, null);

        // Act
        var fagsak = behandling.getFagsak();
        dokumentmottaker.mottaDokument(mottattDokument, fagsak, null);

        // Assert
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForVedlegg(fagsak, behandling, mottattDokument);
    }

    @Test
    void skal_opprette_task_for_å_vurdere_dokument_når_det_ikke_er_en_søknad_men_har_behandling_på_saken_og_komplett() {
        // Arrange
        var dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBehandlendeEnhet(ENHET.enhetId())
                .medBehandlingStegStart(BehandlingStegType.FORESLÅ_VEDTAK);
        var behandling = scenario.lagre(repositoryProvider);

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, behandling.getFagsakId(), "", now(), true, null);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), null);

        // Assert
        verify(dokumentmottakerFelles, times(0)).opprettTaskForÅVurdereDokument(behandling.getFagsak(), behandling, mottattDokument);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller).persisterDokumentOgVurderKompletthet(behandling, mottattDokument);
        verifyNoInteractions(taskTjeneste);
    }

    /**
     * Vurder dokument oppgaver skal ikke bruke behandlende enhetsid fra klager. Er
     * behandlingen en klage skal vi hente ut behandlende enhet fra siste behandling
     * som ikke er klage
     */
    @Test
    void skal_opprette_task_for_å_vurdere_dokument_når_det_ikke_er_en_søknad_men_har_behandling_på_saken_hent_behandlende_enhet_fra_ikke_klagebehandling() {
        var førstegangssøknadEnhetsId = "4833";
        var klageEnhetsId = "4292";

        // Arrange
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(
            ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlendeEnhet(førstegangssøknadEnhetsId)).medBehandlendeEnhet(klageEnhetsId);
        var klageBehandling = scenario.lagre(repositoryProvider, klageRepository);

        var fagsakId = scenario.getFagsak().getId();
        var dokumentTypeId = DokumentTypeId.DOKUMENTASJON_AV_OMSORGSOVERTAKELSE;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, klageBehandling.getFagsak(), null);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(klageBehandling.getFagsak(), klageBehandling, mottattDokument);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(klageBehandling, mottattDokument);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(OpprettOppgaveVurderDokumentTask.class));
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo(førstegangssøknadEnhetsId);
        // Lik enheten som ble satt på behandlingen
    }

    @Test
    void skal_ikke_opprette_køet_behandling_når_ingen_tidligere_behandling() {
        // Arrange - opprette fagsak uten behandling
        var fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), RelasjonsRolleType.MORA, NavBrukerKjønn.KVINNE, new Saksnummer("9999"),
                repositoryProvider.getFagsakRepository());

        // Act - send inn endringssøknad
        var fagsakId = fagsak.getId();
        var dokumentTypeId = DokumentTypeId.ANNET;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);
        dokumentmottaker.mottaDokumentForKøetBehandling(mottattDokument, fagsak, BehandlingÅrsakType.RE_ANNET);

        // Assert - verifiser flyt
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(null, mottattDokument);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(fagsak, null, mottattDokument);
    }

    @Test
    void skal_opprette_task_for_å_vurdere_dokument_når_dokumenttype_er_udefinert() {
        var førstegangssøknadEnhetsId = ENHET.enhetId();
        var klageEnhetsId = BehandlendeEnhetTjeneste.getKlageInstans().enhetId();

        // Arrange
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(
            ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlendeEnhet(førstegangssøknadEnhetsId)).medBehandlendeEnhet(klageEnhetsId);
        var klageBehandling = scenario.lagre(repositoryProvider, klageRepository);

        var fagsakId = scenario.getFagsak().getId();
        var dokumentTypeId = DokumentTypeId.UDEFINERT;

        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(dokumentTypeId, fagsakId, "", now(), true, null);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, klageBehandling.getFagsak(), null);

        // Assert
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereDokument(klageBehandling.getFagsak(), klageBehandling, mottattDokument);
        verifyNoInteractions(mottatteDokumentTjeneste);

        // Verifiser at korrekt prosesstask for vurder dokument blir opprettet
        verify(kompletthetskontroller, times(0)).persisterDokumentOgVurderKompletthet(klageBehandling, mottattDokument);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskData = captor.getValue();
        assertThat(prosessTaskData.taskType()).isEqualTo(TaskType.forProsessTask(OpprettOppgaveVurderDokumentTask.class));
        // Lik enheten som ble satt på behandlingen
        assertThat(prosessTaskData.getPropertyValue(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET)).isEqualTo(førstegangssøknadEnhetsId);
    }

    private Fagsak nyMorFødselFagsak() {
        return ScenarioMorSøkerEngangsstønad.forFødselUtenSøknad().lagreFagsak(repositoryProvider);
    }
}
