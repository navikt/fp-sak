package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.domene.vedtak.observer.RestRePubliserVedtattYtelseHendelseTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask(value = "vedtak.republiser.dag", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class ReLagreVedtakDagTask extends GenerellProsessTask {

    public static final String LOG_FOM_KEY = "logfom";
    public static final String LOG_TOM_KEY = "logtom";


    private InformasjonssakRepository informasjonssakRepository;
    private ProsessTaskTjeneste taskTjeneste;

    ReLagreVedtakDagTask() {
        // for CDI proxy
    }

    @Inject
    public ReLagreVedtakDagTask(InformasjonssakRepository informasjonssakRepository,
                                ProsessTaskTjeneste taskTjeneste) {
        super();
        this.informasjonssakRepository = informasjonssakRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var fom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_FOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        var tom = LocalDate.parse(prosessTaskData.getPropertyValue(LOG_TOM_KEY), DateTimeFormatter.ISO_LOCAL_DATE);


        // Finner alle behandlinger med vedtaksdato innen intervall (evt med gitt saksnummer) - tidligste dato = tidligeste dato med utbetaling
        var saker = informasjonssakRepository.finnSakerSisteVedtakInnenIntervallMedKunUtbetalte(fom, tom, null);
        var spread = 3599;
        var baseline = LocalDateTime.now();
        if (MDCOperations.getCallId() == null) MDCOperations.putCallId();
        var callId = MDCOperations.getCallId();
        int suffix = 1;
        for (var o : saker) {
            var taskData = ProsessTaskData.forProsessTask(RestRePubliserVedtattYtelseHendelseTask.class);
            taskData.setProperty(RestRePubliserVedtattYtelseHendelseTask.KEY, o.getBehandlingId().toString());
            taskData.setNesteKjøringEtter(baseline.plusSeconds(LocalDateTime.now().getNano() % spread));
            taskData.setCallId(callId + "_" + suffix);
            taskData.setPrioritet(50);
            taskTjeneste.lagre(taskData);
            suffix++;
        }
    }

}
