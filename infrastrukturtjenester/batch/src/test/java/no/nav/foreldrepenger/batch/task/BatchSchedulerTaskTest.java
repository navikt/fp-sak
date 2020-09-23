package no.nav.foreldrepenger.batch.task;

import static no.nav.foreldrepenger.batch.task.BatchSchedulerTask.TASKTYPE;
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

public class BatchSchedulerTaskTest {

    private BatchSchedulerTask task;
    private BatchSupportTjenesteTest testsupport;
    private ProsessTaskData taskData = new ProsessTaskData(TASKTYPE);

    @BeforeEach
    public void setup() {
        testsupport = new BatchSupportTjenesteTest();
        task = new BatchSchedulerTask(testsupport);
    }

    @Test
    public void normal_dag_skal_ha_7_tasks_med_antalldager() {
        // Arrange
        task.doTask(taskData);
        var props = testsupport.getTaskDataList();
        List<String> matches = props.stream()
                .map(t -> t.getProperty(BatchRunnerTask.BATCH_PARAMS))
                .filter(Objects::nonNull)
                .filter(s -> s.matches("[a-zA-Z,= ]*antallDager=[1-7]"))
                .collect(Collectors.toList());
        if (props.size() > 1) {
            System.out.println(matches);
            assertThat(matches.size()).isEqualTo(7); // Antall dagsensitive batcher.
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
            gruppe.getTasks().stream().map(ProsessTaskGruppe.Entry::getTask).map(ProsessTaskData::getProperties).forEach(taskDataList::add);
        }

        List<Properties> getTaskDataList() {
            return taskDataList;
        }
    }
}
