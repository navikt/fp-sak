package no.nav.foreldrepenger.mottak.vedtak.avstemming;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskDataBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask(value = "vedtak.overlapp.periode", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VedtakAvstemPeriodeTask extends GenerellProsessTask {

    public static final String LOG_FOM_KEY = "logfom";
    public static final String LOG_TOM_KEY = "logtom";


    private InformasjonssakRepository informasjonssakRepository;
    private ProsessTaskTjeneste taskTjeneste;

    VedtakAvstemPeriodeTask() {
        // for CDI proxy
    }

    @Inject
    public VedtakAvstemPeriodeTask(InformasjonssakRepository informasjonssakRepository,
                                   ProsessTaskTjeneste taskTjeneste) {
        super();
        this.informasjonssakRepository = informasjonssakRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var fom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var tom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var baseline = LocalDateTime.now();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        var gruppe = new ProsessTaskGruppe();
        List<ProsessTaskData> tasks = new ArrayList<>();
        informasjonssakRepository.finnSakerSisteVedtakInnenIntervallMedKunUtbetalte(fom, tom, null).forEach(f -> {
            var task = ProsessTaskDataBuilder.forProsessTask(VedtakOverlappAvstemSakTask.class)
                .medProperty(VedtakOverlappAvstemSakTask.LOG_SAKSNUMMER_KEY, f.getSaksnummer().getVerdi())
                .medProperty(VedtakOverlappAvstemSakTask.LOG_HENDELSE_KEY, OverlappVedtak.HENDELSE_AVSTEM_PERIODE)
                .medNesteKjøringEtter(baseline.plusSeconds(Math.abs(System.nanoTime()) % 239))
                .medCallId(callId + "_" + f.getSaksnummer().getVerdi())
                .medPrioritet(100)
                .build();
            tasks.add(task);
        });
        gruppe.addNesteParallell(tasks);
        taskTjeneste.lagre(gruppe);
    }


}
