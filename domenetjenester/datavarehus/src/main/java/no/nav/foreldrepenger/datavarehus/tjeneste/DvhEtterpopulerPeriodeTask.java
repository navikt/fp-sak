package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.repopulerdvhperiode", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class DvhEtterpopulerPeriodeTask extends GenerellProsessTask {

    public static final String LOG_FOM_KEY = "logfom";
    public static final String LOG_TOM_KEY = "logtom";


    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    DvhEtterpopulerPeriodeTask() {
        // for CDI proxy
    }

    @Inject
    public DvhEtterpopulerPeriodeTask(BehandlingRepository behandlingRepository,
                                      ProsessTaskTjeneste taskTjeneste) {
        super();
        this.behandlingRepository = behandlingRepository;
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
        behandlingRepository.hentÅpneBehandlingerOpprettetIntervall(fom.atStartOfDay(), tom.plusDays(1).atStartOfDay()).forEach(b -> {
            var task = ProsessTaskData.forProsessTask(DvhEtterpopulerBehandlingTask.class);
            task.setBehandling(b.getFagsakId(), b.getId(), b.getAktørId().getId());
            task.setNesteKjøringEtter(baseline.plusSeconds(Math.abs(System.nanoTime()) % 239));
            task.setCallId(callId + "_" + b.getId());
            task.setPrioritet(100);
            tasks.add(task);
        });
        gruppe.addNesteParallell(tasks);
        taskTjeneste.lagre(gruppe);
    }


}
