package no.nav.foreldrepenger.domene.vedtak.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.datavarehus.task.VedtakTilDatavarehusTask;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.ekstern.SettUtbetalingPåVentPrivatArbeidsgiverTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.VurderOppgaveArenaTask;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SettFagsakRelasjonAvslutningsdatoTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
public class OpprettProsessTaskIverksettTest {

    @Mock
    private ProsessTaskTjeneste taskTjeneste;

    private Behandling behandling;
    private OpprettProsessTaskIverksett opprettProsessTaskIverksettFP;

    @BeforeEach
    public void setup() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagMocked();
        opprettProsessTaskIverksettFP = new OpprettProsessTaskIverksett(taskTjeneste);
    }

    @Test
    public void skalIkkeAvslutteOppgave() {
        // Arrange

        // Act
        opprettProsessTaskIverksettFP.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataList = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(TaskType.forProsessTask(AvsluttBehandlingTask.class), TaskType.forProsessTask(SendVedtaksbrevTask.class),
            TaskType.forProsessTask(VurderOgSendØkonomiOppdragTask.class), TaskType.forProsessTask(SettUtbetalingPåVentPrivatArbeidsgiverTask.class),
            TaskType.forProsessTask(VurderOppgaveArenaTask.class), TaskType.forProsessTask(VedtakTilDatavarehusTask.class), TaskType.forProsessTask(SettFagsakRelasjonAvslutningsdatoTask.class));
    }

    @Test
    public void testOpprettIverksettingstasker() {
        // Arrange

        // Act
        opprettProsessTaskIverksettFP.opprettIverksettingTasks(behandling);

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(taskTjeneste).lagre(captor.capture());
        var prosessTaskDataList = captor.getValue().getTasks().stream().map(ProsessTaskGruppe.Entry::task).toList();
        var tasktyper = prosessTaskDataList.stream().map(ProsessTaskData::taskType).collect(Collectors.toList());
        assertThat(tasktyper).contains(TaskType.forProsessTask(AvsluttBehandlingTask.class), TaskType.forProsessTask(SendVedtaksbrevTask.class),
            TaskType.forProsessTask(VurderOgSendØkonomiOppdragTask.class), TaskType.forProsessTask(SettUtbetalingPåVentPrivatArbeidsgiverTask.class),
            TaskType.forProsessTask(VurderOppgaveArenaTask.class), TaskType.forProsessTask(VedtakTilDatavarehusTask.class), TaskType.forProsessTask(SettFagsakRelasjonAvslutningsdatoTask.class));
    }

}
