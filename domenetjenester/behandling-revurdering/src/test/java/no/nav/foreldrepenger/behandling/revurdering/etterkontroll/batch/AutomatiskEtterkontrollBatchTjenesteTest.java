package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.batch.BatchStatus;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;

public class AutomatiskEtterkontrollBatchTjenesteTest {

    private AutomatiskEtterkontrollBatchTjeneste tjeneste;
    private ProsessTaskRepository prosessTaskRepository;

    @BeforeEach
    public void setUp() throws Exception {
        prosessTaskRepository = mock(ProsessTaskRepository.class);
        tjeneste = new AutomatiskEtterkontrollBatchTjeneste(prosessTaskRepository, null);
    }

    @Test
    public void skal_returnere_status_ok_ved_fullført() throws Exception {
        final List<TaskStatus> statuses = Collections.singletonList(new TaskStatus(ProsessTaskStatus.FERDIG, BigDecimal.ONE));
        when(prosessTaskRepository.finnStatusForTaskIGruppe(AutomatiskEtterkontrollTask.TASKTYPE,"1234")).thenReturn(statuses);

        final BatchStatus status = tjeneste.status("1234");

        assertThat(status).isEqualTo(BatchStatus.OK);
    }

    @Test
    public void skal_returnere_status_warning_ved_fullført_med_feilet() throws Exception {
        final List<TaskStatus> statuses = List.of(new TaskStatus(ProsessTaskStatus.FERDIG, BigDecimal.ONE),
                new TaskStatus(ProsessTaskStatus.FEILET, BigDecimal.ONE));
        when(prosessTaskRepository.finnStatusForTaskIGruppe(AutomatiskEtterkontrollTask.TASKTYPE,"1234")).thenReturn(statuses);

        final BatchStatus status = tjeneste.status("1234");

        assertThat(status).isEqualTo(BatchStatus.WARNING);
    }

    @Test
    public void skal_returnere_status_running_ved_ikke_fullført() throws Exception {
        final List<TaskStatus> statuses = List.of(new TaskStatus(ProsessTaskStatus.FERDIG, BigDecimal.ONE),
                new TaskStatus(ProsessTaskStatus.FEILET, BigDecimal.ONE), new TaskStatus(ProsessTaskStatus.KLAR, BigDecimal.TEN));
        when(prosessTaskRepository.finnStatusForTaskIGruppe(AutomatiskEtterkontrollTask.TASKTYPE,"1234")).thenReturn(statuses);

        final BatchStatus status = tjeneste.status("1234");

        assertThat(status).isEqualTo(BatchStatus.RUNNING);
    }
}
