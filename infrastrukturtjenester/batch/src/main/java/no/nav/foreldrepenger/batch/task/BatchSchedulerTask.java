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

/**
 * Enkel scheduler for dagens situasjon der man kjører batcher mandag-fredag og
 * det er noe variasjon i parametere.
 *
 * Kan evt endres slik at BatchSchedulerTask kjører tidlig på døgnet og
 * oppretter dagens batches (hvis ikke tidspunkt passert)
 *
 * Skal man utvide med ukentlige, måndedlige batcher etc bør man se på
 * cron-aktige uttrykk for spesifikasjon av kjøring. FC har implementert et
 * rammeverk på github
 */
@ApplicationScoped
@ProsessTask(value = "batch.scheduler", maxFailedRuns = 1)
public class BatchSchedulerTask implements ProsessTaskHandler {

    public static final String ANT_DAGER = "antallDager=";

    private static final String AVSTEMMING = "BVL001";

    // Ved tidsplanlegging: Husk at fødselshendelser prosesseres 06:30-06:59 (de
    // fleste gir automatisk vedtak) - inntil Leesah/OS
    // Trege kall til skatt i gjenoppta, fødselshendeler og oppdatering/dagsgamle +
    // omfattende query på infobrev
    // Gjenoppta-tasks og oppdatering spres utover 24 min, infobrev 5 min,

    // Andre parametere må stå før antallDager= ....
    private static final List<BatchConfig> BATCH_OPPSETT_ANTALL_DAGER = Arrays.asList(
            new BatchConfig(6, 45, AVSTEMMING, "fagomrade=SVP, antallDager="),
            new BatchConfig(6, 46, AVSTEMMING, "fagomrade=SVPREF, antallDager="),
            new BatchConfig(6, 47, AVSTEMMING, "fagomrade=REFUTG, antallDager="),
            new BatchConfig(6, 48, AVSTEMMING, "fagomrade=FP, antallDager="),
            new BatchConfig(6, 49, AVSTEMMING, "fagomrade=FPREF, antallDager="),
            new BatchConfig(7, 3, "BVL008", ANT_DAGER), // Infobrev far - 7min spread
            new BatchConfig(7, 10, "BVL009", ANT_DAGER) // Infobrev opphold far - 3 min spread
    );

    private static final List<BatchConfig> BATCH_OPPSETT_VIRKEDAGER = Arrays.asList(
            new BatchConfig(1, 58, "BVL010", null), // Oppdatering DVH. Bør kjøre før kl 03-04.
            new BatchConfig(6, 5, "BVL004", null), // Gjenoppta - 24 min spread
            new BatchConfig(6, 51, "BVL005", null), // Kodeverk
            new BatchConfig(7, 0, "BVL002", null), // Etterkontroll
            new BatchConfig(7, 2, "BVL003", null), // Forlengelsesbrev må kjøre noe etter Gjenoppta
            new BatchConfig(7, 1, "BVL006", null), // Fagsakavslutning
            new BatchConfig(7, 15, "BVL007", null), // Oppdatering dagsgamle oppgaver - 24 min spread
            new BatchConfig(7, 45, BatchRunnerTask.BATCH_NAME_RETRY_TASKS, null) // Siste steg
    );

    private final Set<MonthDay> fasteStengteDager = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(5, 1),
            MonthDay.of(5, 17),
            MonthDay.of(12, 25),
            MonthDay.of(12, 26),
            MonthDay.of(12, 31));

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
        var dagensDato = LocalDate.now();
        var dagensUkedag = DayOfWeek.from(dagensDato);

        // Lagre neste instans av daglig scheduler straks over midnatt
        var batchScheduler = ProsessTaskData.forProsessTask(BatchSchedulerTask.class);
        var nesteScheduler = dagensDato.plusDays(1).atStartOfDay().plusHours(1).plusMinutes(1);
        batchScheduler.setNesteKjøringEtter(nesteScheduler);
        var gruppeScheduler = new ProsessTaskGruppe(batchScheduler);
        batchSupportTjeneste.opprettScheduledTasks(gruppeScheduler);

        // Ingenting å kjøre i helgene enn så lenge
        if (HELG.contains(dagensUkedag) || erFastInfotrygdStengtDag(dagensDato)) {
            return;
        }

        var antallDager = Integer.toString(beregnAntallDager(dagensDato));
        List<BatchConfig> batchOppsett = new ArrayList<>(BATCH_OPPSETT_VIRKEDAGER);
        BATCH_OPPSETT_ANTALL_DAGER.stream().map(b -> new BatchConfig(b, antallDager)).forEach(batchOppsett::add);

        if (!batchOppsett.isEmpty()) {
            var batchtasks = batchOppsett.stream()
                    .map(bc -> mapBatchConfigTilBatchRunnerTask(bc, dagensDato))
                    .collect(Collectors.toList());
            var gruppeRunner = new ProsessTaskGruppe();
            gruppeRunner.addNesteParallell(batchtasks);

            batchSupportTjeneste.opprettScheduledTasks(gruppeRunner);
        }
    }

    private static ProsessTaskData mapBatchConfigTilBatchRunnerTask(BatchConfig config, LocalDate dagensDato) {
        var batchRunnerTask = ProsessTaskData.forProsessTask(BatchRunnerTask.class);
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
