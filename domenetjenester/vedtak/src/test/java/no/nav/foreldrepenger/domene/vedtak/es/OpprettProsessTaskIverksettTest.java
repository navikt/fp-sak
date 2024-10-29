package no.nav.foreldrepenger.domene.vedtak.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
class OpprettProsessTaskIverksettTest {

    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;

    @BeforeEach
    void setUp() {
        opprettProsessTaskIverksett = new OpprettProsessTaskIverksett(taskTjeneste);
    }

    @Test
    void skalIkkeAvslutteOppgave() {
        // Arrange

        // Act
        opprettProsessTaskIverksett.opprettIverksettingTasks(opprettBehandling());

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataList = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::taskType).toList();
        assertThat(tasktyper).contains(TaskType.forProsessTask(AvsluttBehandlingTask.class), TaskType.forProsessTask(SendVedtaksbrevTask.class),
            TaskType.forProsessTask(VurderOgSendØkonomiOppdragTask.class));
    }

    @Test
    void testOpprettIverksettingstasker() {
        // Arrange
        var behandling = opprettBehandling();

        // Act
        opprettProsessTaskIverksett.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataList = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::taskType).toList();
        assertThat(tasktyper).contains(TaskType.forProsessTask(AvsluttBehandlingTask.class), TaskType.forProsessTask(SendVedtaksbrevTask.class),
            TaskType.forProsessTask(VurderOgSendØkonomiOppdragTask.class));
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        return scenario.lagMocked();
    }

}
