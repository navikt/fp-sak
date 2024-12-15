package no.nav.foreldrepenger.mottak.vedtak.observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class MottaKlageAnkeVedtakTaskTest extends EntityManagerAwareTest {

    private static final TaskType VKY_TASK_TYPE = TaskType.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);

    @Mock
    private ProsessTaskTjeneste taskTjeneste;


    @Test
    void opprett_task_stadfest_NK() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forStadfestetNK(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasktype = captor.getValue().taskType();
        assertThat(tasktype).isEqualTo(VKY_TASK_TYPE);
    }

    @Test
    void opprett_task_medhold_NFP() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forMedholdNFP(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasktype = captor.getValue().taskType();
        assertThat(tasktype).isEqualTo(VKY_TASK_TYPE);
    }


    @Test
    void opprett_task_medhold_NK() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forMedholdNK(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasktype = captor.getValue().taskType();
        assertThat(tasktype).isEqualTo(VKY_TASK_TYPE);
    }

    @Test
    void opprett_task_opphevet_NK() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forOpphevetNK(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasktype = captor.getValue().taskType();
        assertThat(tasktype).isEqualTo(VKY_TASK_TYPE);
    }

    private MottaKlageAnkeVedtakTask opprettKlageProsessTask(ScenarioKlageEngangsstønad scenario) {
        var behandlingRepository = scenario.mockBehandlingRepositoryProvider().getBehandlingRepository();
        var klageRepository = scenario.getKlageRepository();
        return new MottaKlageAnkeVedtakTask(taskTjeneste, behandlingRepository, null, klageRepository);
    }

    @Test
    void opprett_task_hjemsendt_NK() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forHjemsendtNK(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasktype = captor.getValue().taskType();
        assertThat(tasktype).isEqualTo(VKY_TASK_TYPE);
    }

    @Test
    void opprett_task_avvist_NK() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forAvvistNK(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste).lagre(captor.capture());
        var tasktype = captor.getValue().taskType();
        assertThat(tasktype).isEqualTo(VKY_TASK_TYPE);
    }

    @Test
    void ikke_opprett_task_avvist_NFP() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forAvvistNFP(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void ikke_opprett_task_mangler_resultat() {
        // Arrange
        var abstractScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var scenario = ScenarioKlageEngangsstønad.forUtenVurderingResultat(abstractScenario);
        var behandling = scenario.lagMocked();

        // Act
        var klageAnkeHåndterer = opprettKlageProsessTask(scenario);
        klageAnkeHåndterer.opprettKlageAnkeTasks(behandling);

        // Assert
        verifyNoInteractions(taskTjeneste);
    }



}
