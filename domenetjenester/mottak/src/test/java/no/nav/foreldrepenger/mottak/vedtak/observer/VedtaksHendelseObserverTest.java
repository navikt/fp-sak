package no.nav.foreldrepenger.mottak.vedtak.observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.mottak.vedtak.StartBerørtBehandlingTask;
import no.nav.foreldrepenger.mottak.vedtak.overlapp.VurderOpphørAvYtelserTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class VedtaksHendelseObserverTest extends EntityManagerAwareTest {
    private VedtaksHendelseObserver vedtaksHendelseObserver;

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        behandlingVedtakRepository = new BehandlingVedtakRepository(getEntityManager());
        vedtaksHendelseObserver = new VedtaksHendelseObserver(taskTjeneste);
    }

    @Test
    void opprettRiktigeTasksForFpsakVedtakForeldrepenger() {
        var fpBehandling = lagBehandlingFP();
        var fpYtelse = genererYtelseFpsak(fpBehandling);

        vedtaksHendelseObserver.observerBehandlingVedtakEvent(fpYtelse);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(2)).lagre(captor.capture());
        var prosessTaskDataList = captor.getAllValues();

        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::taskType).toList();
        assertThat(tasktyper).contains(TaskType.forProsessTask(VurderOpphørAvYtelserTask.class), TaskType.forProsessTask(StartBerørtBehandlingTask.class));

    }

    @Test
    void opprettRiktigeTasksForFpsakVedtakSvangerskapspenger() {
        var svpBehandling = lagBehandlingSVP();
        var svpYtelse = genererYtelseFpsak(svpBehandling);

        vedtaksHendelseObserver.observerBehandlingVedtakEvent(svpYtelse);

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataList = captor.getAllValues();

        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::taskType).toList();

        assertThat(tasktyper).contains(TaskType.forProsessTask(VurderOpphørAvYtelserTask.class));
    }

    @Test
    void opprettIngenTasksForFpsakVedtakEngangsstønad() {
        var esBehandling = lagBehandlingES();
        var esYtelse = genererYtelseFpsak(esBehandling);

        vedtaksHendelseObserver.observerBehandlingVedtakEvent(esYtelse);

        verifyNoInteractions(taskTjeneste);
    }

    private Behandling lagBehandlingFP() {
        ScenarioMorSøkerForeldrepenger scenarioFP;
        scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenarioFP.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioFP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioFP.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now()).medIverksettingStatus(IverksettingStatus.IVERKSATT)
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandling = scenarioFP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private Behandling lagBehandlingSVP() {
        ScenarioMorSøkerSvangerskapspenger scenarioSVP;
        scenarioSVP = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenarioSVP.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioSVP.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioSVP.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now()).medIverksettingStatus(IverksettingStatus.IVERKSATT)
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandling = scenarioSVP.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private Behandling lagBehandlingES() {
        ScenarioMorSøkerEngangsstønad scenarioES;
        scenarioES = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenarioES.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        scenarioES.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        scenarioES.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now()).medIverksettingStatus(IverksettingStatus.IVERKSATT)
                .medVedtakResultatType(VedtakResultatType.INNVILGET);

        var behandling = scenarioES.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        return behandling;
    }

    private BehandlingVedtakEvent genererYtelseFpsak(Behandling behandling) {
        var vedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId()).orElseThrow();

        return new BehandlingVedtakEvent(vedtak, behandling);
    }

}
