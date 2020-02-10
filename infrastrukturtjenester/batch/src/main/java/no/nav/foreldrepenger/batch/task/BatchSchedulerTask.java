package no.nav.foreldrepenger.batch.task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;
import no.nav.vedtak.util.FPDateUtil;

/**
 * Enkel scheduler for dagens situasjon der man kjører batcher mandag-fredag og det er noe variasjon i parametere.
 *
 * Kan evt endres slik at BatchSchedulerTask kjører tidlig på døgnet og oppretter dagens batches (hvis ikke tidspunkt passert)
 *
 * Skal man utvide med ukentlige, måndedlige batcher etc bør man se på cron-aktige uttrykk for spesifikasjon av kjøring.
 * FC har implementert et rammeverk på github
 */
@ApplicationScoped
@ProsessTask(BatchSchedulerTask.TASKTYPE)
public class BatchSchedulerTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "batch.scheduler";

    private static final String AVSTEMMING = "BVL001";

    // Andre parametere må stå før antallDager= ....
    private static final List<BatchConfig> BATCH_OPPSETT_ANTALL_DAGER = Arrays.asList(
        new BatchConfig(6, 53, AVSTEMMING, "fagomrade=SVP, antallDager="),
        new BatchConfig(6, 54, AVSTEMMING, "fagomrade=SVPREF, antallDager="),
        new BatchConfig(6, 55, AVSTEMMING, "fagomrade=REFUTG, antallDager="),
        new BatchConfig(6, 56, AVSTEMMING, "fagomrade=FP, antallDager="),
        new BatchConfig(6, 57, AVSTEMMING, "fagomrade=FPREF, antallDager="),
        new BatchConfig(7, 5, "BVL006", "antallDager="), // Fagsakavslutning
        new BatchConfig(7, 7, "BVL008", "antallDager="), // Infobrev far
        new BatchConfig(7, 15, "BVL009", "antallDager=") // Infobrev opphold far
    );

    private static final List<BatchConfig> BATCH_OPPSETT_VIRKEDAGER = Arrays.asList(
        new BatchConfig(6, 45, "BVL007", null), // Oppdatering dagsgamle oppgaver
        new BatchConfig(6, 58, BatchRunnerTask.BATCH_NAME_RETRY_TASKS, null),
        new BatchConfig(6, 59, "BVL005", null), // Kodeverk
        new BatchConfig(7, 0, "BVL004", null), // Gjenoppta
        new BatchConfig(7, 4, "BVL002", null), // Etterkontroll
        new BatchConfig(7, 6, "BVL003", null)  // Forlengelsesbrev må kjøre noe etter Gjenoppta
    );

    private final Set<MonthDay> fasteStengteDager = Set.of(
        MonthDay.of(1, 1),
        MonthDay.of(5, 1),
        MonthDay.of(5, 17),
        MonthDay.of(12, 25),
        MonthDay.of(12, 26),
        MonthDay.of(12, 30), // TODO midlertidig pga KOR 2020
        MonthDay.of(12, 31)
    );

    private static final Set<DayOfWeek> HELG = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private BatchSupportTjeneste batchSupportTjeneste;

    BatchSchedulerTask() {
        // for CDI proxy
    }

    @Inject
    public BatchSchedulerTask(BatchSupportTjeneste batchSupportTjeneste) {
        this.batchSupportTjeneste = batchSupportTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        LocalDate dagensDato = FPDateUtil.iDag();
        DayOfWeek dagensUkedag = DayOfWeek.from(dagensDato);

        // Lagre neste instans av daglig scheduler straks over midnatt
        ProsessTaskData batchScheduler = new ProsessTaskData(BatchSchedulerTask.TASKTYPE);
        LocalDateTime nesteScheduler = dagensDato.plusDays(1).atStartOfDay().plusHours(1).plusMinutes(1);
        batchScheduler.setNesteKjøringEtter(nesteScheduler);
        ProsessTaskGruppe gruppeScheduler = new ProsessTaskGruppe(batchScheduler);
        batchSupportTjeneste.opprettScheduledTasks(gruppeScheduler);

        // Ingenting å kjøre i helgene enn så lenge
        if (HELG.contains(dagensUkedag) || erFastInfotrygdStengtDag(dagensDato)) {
            return;
        }

        String antallDager = Integer.toString(beregnAntallDager(dagensDato));
        List<BatchConfig> batchOppsett = new ArrayList<>(BATCH_OPPSETT_VIRKEDAGER);
        BATCH_OPPSETT_ANTALL_DAGER.stream().map(b -> new BatchConfig(b, antallDager)).forEach(batchOppsett::add);

        if (!batchOppsett.isEmpty()) {
            List<ProsessTaskData> batchtasks = batchOppsett.stream()
                .map(bc -> mapBatchConfigTilBatchRunnerTask(bc, dagensDato))
                .collect(Collectors.toList());
            ProsessTaskGruppe gruppeRunner = new ProsessTaskGruppe();
            gruppeRunner.addNesteParallell(batchtasks);

            batchSupportTjeneste.opprettScheduledTasks(gruppeRunner);
        }
    }

    private ProsessTaskData mapBatchConfigTilBatchRunnerTask(BatchConfig config, LocalDate dagensDato) {
        ProsessTaskData batchRunnerTask = new ProsessTaskData(BatchRunnerTask.TASKTYPE);
        batchRunnerTask.setProperty(BatchRunnerTask.BATCH_NAME, config.getName());
        if (config.getParams() != null) {
            batchRunnerTask.setProperty(BatchRunnerTask.BATCH_PARAMS, config.getParams());
        }
        batchRunnerTask.setProperty(BatchRunnerTask.BATCH_RUN_DATE, dagensDato.toString());
        batchRunnerTask.setNesteKjøringEtter(LocalDateTime.of(dagensDato, config.getKjøreTidspunkt()));
        return batchRunnerTask;
    }

    private int beregnAntallDager(LocalDate dato) {
        var sjekkDato = dato.minusDays(1);
        if (HELG.contains(DayOfWeek.from(sjekkDato)) || erFastInfotrygdStengtDag(sjekkDato)) {
            return 1 + beregnAntallDager(sjekkDato);
        }
        return 1;
    }

    private boolean erFastInfotrygdStengtDag(LocalDate dato) {
        return fasteStengteDager.contains(MonthDay.from(dato));
    }
}
