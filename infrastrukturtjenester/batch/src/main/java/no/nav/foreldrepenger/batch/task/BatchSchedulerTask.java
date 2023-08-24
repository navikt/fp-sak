package no.nav.foreldrepenger.batch.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.batch.BatchSupportTjeneste;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import java.time.*;
import java.util.*;

import static no.nav.foreldrepenger.batch.BatchTjeneste.FAGOMRÅDE_KEY;

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

    private static final String AVSTEMMING = "BVL001";

    // Ved tidsplanlegging: Husk at nattens fødselshendelser prosesseres 06:30-06:59 (de fleste gir automatisk vedtak)
    // Trege kall til skatt i gjenoppta, fødselshendeler og oppdatering/dagsgamle + omfattende query på infobrev
    // Gjenoppta-tasks og oppdatering spres utover 24 min, infobrev 5 min,

    // Her injiseres antallDager=N ut fra bereging gitt helg og helligdag ....
    private static final List<BatchConfig> BATCH_OPPSETT_ANTALL_DAGER = List.of(
            new BatchConfig(6, 45, AVSTEMMING, FAGOMRÅDE_KEY, "SVP"),
            new BatchConfig(6, 46, AVSTEMMING, FAGOMRÅDE_KEY, "SVPREF"),
            new BatchConfig(6, 47, AVSTEMMING, FAGOMRÅDE_KEY, "REFUTG"),
            new BatchConfig(6, 48, AVSTEMMING, FAGOMRÅDE_KEY, "FP"),
            new BatchConfig(6, 49, AVSTEMMING, FAGOMRÅDE_KEY, "FPREF"),
            new BatchConfig(7, 3, "BVL008"), // Infobrev far - 7min spread
            new BatchConfig(7, 13, "BVL011") // Infobrev far påminnelse - 7min spread
    );

    // Skal kjøres hver ukedag
    private static final List<BatchConfig> BATCH_OPPSETT_VIRKEDAGER = List.of(
            new BatchConfig(1, 58, "BVL010"), // Oppdatering DVH. Bør kjøre før kl 03-04.
            new BatchConfig(6, 5, "BVL004"), // Gjenoppta - 24 min spread
            new BatchConfig(7, 0, "BVL002"), // Etterkontroll
            new BatchConfig(7, 1, "BVL006") // Fagsakavslutning
            // new BatchConfig(7, 49, "BVL007", null) // Gjenoppliv åpne behandlinger uten åpent aksjonspunkt - enable etter sjekk av status
    );

    private static final List<DagligTaskConfig> TASKS_VIRKEDAGER = List.of(
        new DagligTaskConfig(1, 59, SlettGamleTask.class),
        new DagligTaskConfig(7, 45, RetryFeiletTask.class) // Siste steg - etter batch virkedager
    );

    // Skal kjøres enkelte ukedager
    private static final Map<DayOfWeek, List<BatchConfig>> BATCH_OPPSETT_UKEDAG = Map.of(
        DayOfWeek.WEDNESDAY, List.of(new BatchConfig(11, 30, "BVL005")), // Kodeverk
        DayOfWeek.TUESDAY, List.of(new BatchConfig(11, 30, "BVL012") ) // Avslutter saker med kobling til annen part og enkeltopphør
    );

    private final Set<MonthDay> fasteStengteDager = Set.of(
            MonthDay.of(1, 1),
            MonthDay.of(5, 1),
            MonthDay.of(5, 17),
            MonthDay.of(12, 25),
            MonthDay.of(12, 26),
            MonthDay.of(12, 31));

    private final Map<Integer, List<LocalDate>> bevegeligeHelligdager = new HashMap<>();

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
        doTaskForDato(prosessTaskData, LocalDate.now());
    }

    void doTaskForDato(ProsessTaskData prosessTaskData, LocalDate dagensDato) {
        var dagensUkedag = DayOfWeek.from(dagensDato);

        // Lagre neste instans av daglig scheduler straks over midnatt
        var batchScheduler = ProsessTaskData.forProsessTask(BatchSchedulerTask.class);
        var nesteScheduler = dagensDato.plusDays(1).atStartOfDay().plusHours(1).plusMinutes(1);
        batchScheduler.setNesteKjøringEtter(nesteScheduler);
        var gruppeScheduler = new ProsessTaskGruppe(batchScheduler);
        batchSupportTjeneste.opprettScheduledTasks(gruppeScheduler);

        // Ingenting å kjøre i helgene enn så lenge
        if (HELG.contains(dagensUkedag) || erStengtDag(dagensDato)) {
            return;
        }

        List<BatchConfig> batchOppsett = new ArrayList<>(BATCH_OPPSETT_VIRKEDAGER);
        batchOppsett.addAll(BATCH_OPPSETT_UKEDAG.getOrDefault(dagensUkedag, List.of()));
        var antallDager = String.valueOf(beregnAntallDager(dagensDato));
        BATCH_OPPSETT_ANTALL_DAGER.stream().map(b -> BatchConfig.appendAntallDager(b, antallDager)).forEach(batchOppsett::add);

        if (!batchOppsett.isEmpty()) {
            var parallelle = new ArrayList<>(TASKS_VIRKEDAGER.stream().map(tc -> mapDagligTaskConfigTilTask(tc, dagensDato)).toList());
            var batchtasks = batchOppsett.stream()
                    .map(bc -> mapBatchConfigTilBatchRunnerTask(bc, dagensDato))
                    .toList();
            parallelle.addAll(batchtasks);
            var gruppeRunner = new ProsessTaskGruppe();
            gruppeRunner.addNesteParallell(parallelle);

            batchSupportTjeneste.opprettScheduledTasks(gruppeRunner);
        }
    }

    private static ProsessTaskData mapDagligTaskConfigTilTask(DagligTaskConfig taskConfig, LocalDate dagensDato) {
        var task = ProsessTaskData.forProsessTask(taskConfig.tClass);
        task.setNesteKjøringEtter(LocalDateTime.of(dagensDato, taskConfig.getKjøreTidspunkt()));
        return task;
    }

    private static ProsessTaskData mapBatchConfigTilBatchRunnerTask(BatchConfig config, LocalDate dagensDato) {
        var batchRunnerTask = ProsessTaskData.forProsessTask(BatchRunnerTask.class);
        batchRunnerTask.setProperty(BatchRunnerTask.BATCH_NAME, config.batchName());
        if (config.params() != null) {
            config.params().forEach(batchRunnerTask::setProperty);
        }
        batchRunnerTask.setProperty(BatchRunnerTask.BATCH_RUN_DATE, dagensDato.toString());
        batchRunnerTask.setNesteKjøringEtter(LocalDateTime.of(dagensDato, config.getKjøreTidspunkt()));
        return batchRunnerTask;
    }

    private int beregnAntallDager(LocalDate dato) {
        var sjekkDato = dato.minusDays(1);
        if (HELG.contains(DayOfWeek.from(sjekkDato)) || erStengtDag(sjekkDato)) {
            return 1 + beregnAntallDager(sjekkDato);
        }
        return 1;
    }

    private boolean erStengtDag(LocalDate dato) {
        if (bevegeligeHelligdager.isEmpty() || bevegeligeHelligdager.get(dato.getYear()) == null) {
            bevegeligeHelligdager.put(dato.getYear(), bevegeligeHelligdager(dato));
        }
        return fasteStengteDager.contains(MonthDay.from(dato)) || bevegeligeHelligdager.get(dato.getYear()).contains(dato);
    }

    private record BatchConfig(int time, int minutt, String batchName, Map<String, String> params) {

        BatchConfig(int time, int minutt, String batchName) {
            this(time, minutt, batchName, Map.of());
        }

        BatchConfig(int time, int minutt, String batchName, String key, String value) {
            this(time, minutt, batchName, Map.of(key, value));
        }

        BatchConfig(BatchConfig config, Map<String, String> params) {
            this(config.time(), config.minutt(), config.batchName(), params);
        }

        static BatchConfig appendAntallDager(BatchConfig config, String value) {
            var params = new LinkedHashMap<>(config.params() != null ? config.params : Map.of());
            params.put(BatchTjeneste.ANTALL_DAGER_KEY, value);
            return new BatchConfig(config, params);
        }

        LocalTime getKjøreTidspunkt() {
            return LocalTime.of(time(), minutt());
        }

    }

    private record DagligTaskConfig(int time, int minutt, Class<? extends ProsessTaskHandler> tClass) {

        LocalTime getKjøreTidspunkt() {
            return LocalTime.of(time(), minutt());
        }
    }

    private List<LocalDate> bevegeligeHelligdager(LocalDate dato) {
        var helligdager = new ArrayList<LocalDate>();
        var påskedag = utledPåskedag(dato.getYear());
        helligdager.add(påskedag.minusDays(3)); // Skjærtorsdag
        helligdager.add(påskedag.minusDays(2)); // Langfredag
        helligdager.add(påskedag.plusDays(1)); // Andre påskedag
        helligdager.add(påskedag.plusDays(39)); // Himmelfarten
        helligdager.add(påskedag.plusDays(50)); // Andre pinsedag
        return helligdager;
    }

    private static LocalDate utledPåskedag(int år) {
        var a = år % 19;
        var b = år / 100;
        var c = år % 100;
        var d = b / 4;
        var e = b % 4;
        var f = (b + 8) / 25;
        var g = (b - f + 1) / 3;
        var h = (19 * a + b - d - g + 15) % 30;
        var i = c / 4;
        var k = c % 4;
        var l = (32 + 2 * e + 2 * i - h - k) % 7;
        var m = (a + 11 * h + 22 * l) / 451;
        var n = (h + l - 7 * m + 114) / 31; // Tallet på måneden
        var p = (h + l - 7 * m + 114) % 31; // Tallet på dagen

        return LocalDate.of(år, n, p + 1);
    }
}
