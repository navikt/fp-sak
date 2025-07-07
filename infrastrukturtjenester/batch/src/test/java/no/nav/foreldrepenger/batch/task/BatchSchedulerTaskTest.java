package no.nav.foreldrepenger.batch.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

class BatchSchedulerTaskTest {

    private BatchSchedulerTask task;
    private BatchSupportTjenesteTest testsupport;
    private final ProsessTaskData taskData = ProsessTaskData.forProsessTask(BatchSchedulerTask.class);
    private final LocalDate enVanligUkedag = LocalDate.of(2023, Month.MAY, 15);
    private final LocalDate enHelgeDag = LocalDate.of(2023, Month.MAY, 13);
    private final LocalDate enStengtDag = LocalDate.of(2023, Month.MAY, 17);

    @BeforeEach
    void setup() {
        testsupport = new BatchSupportTjenesteTest();
        task = new BatchSchedulerTask(testsupport);
    }

    @Test
    void normal_dag_skal_ha_8_tasks_med_antalldager() {
        // Arrange
        task.doTaskForDato(taskData, enVanligUkedag);
        var props = testsupport.getTaskDataList();
        var matches = props.stream()
                .map(t -> t.getProperty(BatchTjeneste.ANTALL_DAGER_KEY))
                .filter(Objects::nonNull)
                .filter(s -> s.matches("[1-7]"))
                .toList();
        assertThat(matches).hasSize(7); // Antall dagsensitive batcher.
    }

    @Test
    void normal_dag_skal_ha_5_tasks_med_fagomraade() {
        // Arrange
        task.doTaskForDato(taskData, enVanligUkedag);
        var props = testsupport.getTaskDataList();
        var matches = props.stream()
            .map(t -> t.getProperty(BatchTjeneste.FAGOMRÅDE_KEY))
            .filter(Objects::nonNull)
            .toList();
        assertThat(matches).hasSize(5); // Antall avstemminger.
    }

    @Test
    void helg_skal_ikke_ha_tasks_med_antalldager() {
        // Arrange
        task.doTaskForDato(taskData, enHelgeDag);
        var props = testsupport.getTaskDataList();
        var matches = props.stream()
            .map(t -> t.getProperty(BatchTjeneste.ANTALL_DAGER_KEY))
            .filter(Objects::nonNull)
            .filter(s -> s.matches("[1-7]"))
            .toList();
        assertThat(matches).isEmpty();
    }

    @Test
    void stengt_dag_skal_ikke_ha_tasks_med_antalldager() {
        // Arrange
        task.doTaskForDato(taskData, enStengtDag);
        var props = testsupport.getTaskDataList();
        var matches = props.stream()
            .map(t -> t.getProperty(BatchTjeneste.ANTALL_DAGER_KEY))
            .filter(Objects::nonNull)
            .filter(s -> s.matches("[1-7]"))
            .toList();
        assertThat(matches).isEmpty();
    }

    private static class BatchSupportTjenesteTest extends BatchSupportTjeneste {
        private final List<Properties> taskDataList;

        BatchSupportTjenesteTest() {
            super();
            taskDataList = new ArrayList<>();
        }

        @Override
        public void opprettScheduledTasks(ProsessTaskGruppe gruppe) {
            gruppe.getTasks().stream().map(ProsessTaskGruppe.Entry::task).map(ProsessTaskData::getProperties).forEach(taskDataList::add);
        }

        List<Properties> getTaskDataList() {
            return taskDataList;
        }
    }
}
