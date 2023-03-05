package no.nav.foreldrepenger.batch.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;

class BatchSchedulerTaskTest {

    private BatchSchedulerTask task;
    private BatchSupportTjenesteTest testsupport;
    private ProsessTaskData taskData = ProsessTaskData.forProsessTask(BatchSchedulerTask.class);

    @BeforeEach
    public void setup() {
        testsupport = new BatchSupportTjenesteTest();
        task = new BatchSchedulerTask(testsupport);
    }

    @Test
    void normal_dag_skal_ha_8_tasks_med_antalldager() {
        // Arrange
        task.doTask(taskData);
        var props = testsupport.getTaskDataList();
        var matches = props.stream()
                .map(t -> t.getProperty(BatchRunnerTask.BATCH_PARAMS))
                .filter(Objects::nonNull)
                .filter(s -> s.matches("[a-zA-Z,= ]*antallDager=[1-7]"))
                .collect(Collectors.toList());
        if (props.size() > 1) {
            System.out.println(matches);
            assertThat(matches).hasSize(7); // Antall dagsensitive batcher.
        }
    }

    private static class BatchSupportTjenesteTest extends BatchSupportTjeneste {
        private List<Properties> taskDataList;

        BatchSupportTjenesteTest() { // NOSONAR
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
