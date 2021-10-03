package no.nav.foreldrepenger.domene.vedtak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
public class OpprettProsessTaskIverksettKlageTest extends EntityManagerAwareTest {

    private static final TaskType AVSLUTT_BEHANDLING_TASK = TaskType.forProsessTask(AvsluttBehandlingTask.class);
    private static final TaskType VEDTAKSBREV_TASK = TaskType.forProsessTask(SendVedtaksbrevTask.class);
    private static final TaskType AVSLUTT_OPPGAVE_TASK = TaskType.forProsessTask(AvsluttOppgaveTask.class);

    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;


    @Test
    public void testOpprettIverksettingstaskerForKlagebehandling() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, AVSLUTT_OPPGAVE_TASK);
    }

    @Test
    public void testOpprettIverksettingstaskerForKlagebehandlingAlternativ() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();

        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, AVSLUTT_OPPGAVE_TASK);
    }


    @Test
    public void skalIkkeAvslutteOppgaveForKlagebehandling() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK);
    }

    @Test
    public void testOpprettIverksettingstaskerForKlagebehandlingMedMedhold() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forMedholdNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, AVSLUTT_OPPGAVE_TASK, TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class));
    }

    @Test
    public void skalIkkeAvslutteOppgaveForKlagebehandlingMedMedhold() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forMedholdNK(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class));
    }

    @Test
    public void skalOppretteOppgaveVurderKonsekvensTaskForKlagebehandlingMedOpphevetNK() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forOpphevetNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, AVSLUTT_OPPGAVE_TASK, TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class));
    }

    private OpprettProsessTaskIverksett opprettKlageProsessTask(ScenarioKlageEngangsstønad scenario) {
        var behandlingRepository = scenario.mockBehandlingRepositoryProvider().getBehandlingRepository();
        var klageRepository = scenario.getKlageRepository();
        return new OpprettProsessTaskIverksett(taskTjeneste, behandlingRepository, null, klageRepository, oppgaveTjeneste);
    }

    @Test
    public void skalOppretteOppgaveVurderKonsekvensTaskForKlagebehandlingMedHjemsendtNK() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forHjemsendtNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, AVSLUTT_OPPGAVE_TASK, TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class));
    }

    @Test
    public void skalIkkeOppretteOppgaveVurderKonsekvensTaskNårVurderingsresultatErIkkeMedholdIKlageEllerOpphevetYtelsesvedtak() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, AVSLUTT_OPPGAVE_TASK);
    }

    @Test
    public void skalIkkeOppretteOppgaveVurderKonsekvensTaskNårKlageErUtenVurderingsresultat() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(abstractScenario);
        var behandling = scenario.lagMocked();
        mockOpprettTaskAvsluttOppgave(behandling);

        // Act
        var opprettProsessTaskIverksettKlage = opprettKlageProsessTask(scenario);
        opprettProsessTaskIverksettKlage.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var resultat = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = resultat.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(AVSLUTT_BEHANDLING_TASK, VEDTAKSBREV_TASK, AVSLUTT_OPPGAVE_TASK);
    }


    private void mockOpprettTaskAvsluttOppgave(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(AvsluttOppgaveTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        OppgaveTjeneste.setOppgaveId(prosessTaskData, "1001");
        when(oppgaveTjeneste.opprettTaskAvsluttOppgave(any(Behandling.class), any(OppgaveÅrsak.class), anyBoolean())).thenReturn(Optional.of(prosessTaskData));
    }

}
