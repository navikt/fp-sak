package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.AvsluttOppgaveTaskProperties;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class OpprettProsessTaskIverksettKlageTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private ProsessTaskRepository prosessTaskRepository = new ProsessTaskRepositoryImpl(entityManager, null, null);

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;

    private Behandling behandling;

    @Test
    public void testOpprettIverksettingstaskerForKlagebehandling() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave();

        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTaskProperties.TASKTYPE);
    }

    @Test
    public void testOpprettIverksettingstaskerForKlagebehandlingAlternativ() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave();

        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTaskProperties.TASKTYPE);
    }


    @Test
    public void skalIkkeAvslutteOppgaveForKlagebehandling() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        behandling = scenario.lagMocked();

        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

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
        behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave();
        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTaskProperties.TASKTYPE, OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
    }

    @Test
    public void skalIkkeAvslutteOppgaveForKlagebehandlingMedMedhold() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forMedholdNK(abstractScenario);
        behandling = scenario.lagMocked();
        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

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
        behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave();
        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            AvsluttOppgaveTaskProperties.TASKTYPE, OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
    }

    private OpprettProsessTaskIverksettKlage opprettKlageProsessTask(ScenarioKlageEngangsstønad scenario) {
        var behandlingRepository = scenario.mockBehandlingRepositoryProvider().getBehandlingRepository();
        var klageRepository = scenario.getKlageRepository();
        return new OpprettProsessTaskIverksettKlage(behandlingRepository, klageRepository, prosessTaskRepository, oppgaveTjeneste);
    }

    @Test
    public void skalOppretteOppgaveVurderKonsekvensTaskForKlagebehandlingMedHjemsendtNK() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forHjemsendtNK(abstractScenario);
        behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave();
        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE,
            AvsluttOppgaveTaskProperties.TASKTYPE, OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
    }

    @Test
    public void skalIkkeOppretteOppgaveVurderKonsekvensTaskNårVurderingsresultatErIkkeMedholdIKlageEllerOpphevetYtelsesvedtak() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forAvvistNK(abstractScenario);
        behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave();
        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTaskProperties.TASKTYPE);
    }

    @Test
    public void skalIkkeOppretteOppgaveVurderKonsekvensTaskNårKlageErUtenVurderingsresultat() {
        // Arrange
        ScenarioMorSøkerEngangsstønad abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(abstractScenario);
        behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave();
        List<ProsessTaskData> resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);

        // Act
        OpprettProsessTaskIverksett opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingstasker(behandling);

        // Assert
        resultat = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        List<String> tasktyper = resultat.stream().map(ProsessTaskData::getTaskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AvsluttBehandlingTask.TASKTYPE, SendVedtaksbrevTask.TASKTYPE, AvsluttOppgaveTaskProperties.TASKTYPE);
    }


    private void mockOpprettTaskAvsluttOppgave() {
        ProsessTaskData prosessTaskData = new ProsessTaskData(AvsluttOppgaveTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setOppgaveId("1001");
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.of(prosessTaskData));
    }

}
