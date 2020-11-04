package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

@ExtendWith(MockitoExtension.class)
public class OpprettProsessTaskIverksettKlageTest extends EntityManagerAwareTest {

    private ProsessTaskRepository prosessTaskRepository;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    @BeforeEach
    void setUp() {
        prosessTaskRepository = new ProsessTaskRepositoryImpl(getEntityManager(), null, null);
    }

    @Test
    public void testOpprettIverksettingstaskerForKlagebehandling() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTask.TASKTYPE);
    }

    @Test
    public void testOpprettIverksettingstaskerForKlagebehandlingAlternativ() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTask.TASKTYPE);
    }


    @Test
    public void skalIkkeAvslutteOppgaveForKlagebehandling() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        var behandling = scenario.lagMocked();

        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE);
    }

    @Test
    public void testOpprettIverksettingstaskerForKlagebehandlingMedMedhold() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forMedholdNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);
        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTask.TASKTYPE, OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
    }

    @Test
    public void skalIkkeAvslutteOppgaveForKlagebehandlingMedMedhold() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forMedholdNK(abstractScenario);
        var behandling = scenario.lagMocked();
        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
    }

    @Test
    public void skalOppretteOppgaveVurderKonsekvensTaskForKlagebehandlingMedOpphevetNK() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forOpphevetNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);
        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            AvsluttOppgaveTask.TASKTYPE, OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
    }

    private OpprettProsessTaskIverksett opprettKlageProsessTask(ScenarioKlageEngangsstønad scenario) {
        var behandlingRepository = scenario.mockBehandlingRepositoryProvider().getBehandlingRepository();
        var klageRepository = scenario.getKlageRepository();
        return new OpprettProsessTaskIverksett(prosessTaskRepository, behandlingRepository, null, klageRepository, oppgaveTjeneste);
    }

    @Test
    public void skalOppretteOppgaveVurderKonsekvensTaskForKlagebehandlingMedHjemsendtNK() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forHjemsendtNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);
        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            AvsluttOppgaveTask.TASKTYPE, OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
    }

    @Test
    public void skalIkkeOppretteOppgaveVurderKonsekvensTaskNårVurderingsresultatErIkkeMedholdIKlageEllerOpphevetYtelsesvedtak() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forAvvistNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);
        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTask.TASKTYPE);
    }

    @Test
    public void skalIkkeOppretteOppgaveVurderKonsekvensTaskNårKlageErUtenVurderingsresultat() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);
        List<ProsessTaskData> resultat;

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTask.TASKTYPE);
    }


    private void mockOpprettTaskAvsluttOppgave(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AvsluttOppgaveTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setOppgaveId("1001");
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.of(prosessTaskData));
    }

}
