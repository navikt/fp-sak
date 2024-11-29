package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskGjenopptagelseTjeneste.class);

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            ProsessTaskTjeneste taskTjeneste,
                                            BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.taskTjeneste = taskTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    public AutomatiskGjenopptagelseTjeneste() {
        // for CDI
    }

    public String gjenopptaBehandlinger() {
        var callId = ensureCallId();
        LOG.info("BATCH Gjenoppta inngang");
        var feileteBehandlinger = behandlingProsesseringTjeneste.behandlingerMedFeiletProsessTask();
        var behandlingListe = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();
        var dato = LocalDate.now();
        var baseline = LocalTime.now();
        LOG.info("BATCH Gjenoppta fant {} behandlinger", behandlingListe.size());
        behandlingListe.stream()
            .filter(b -> !feileteBehandlinger.contains(b.getId()))
            .forEach(behandling -> opprettProsessTasks(behandling, callId, dato, baseline, 1439));
        LOG.info("BATCH Gjenoppta utgang");
        return  "-" + behandlingListe.size();
    }

    private void opprettProsessTasks(Behandling behandling, String callId, LocalDate dato, LocalTime baseline, int spread) {
        var nesteKjøring = LocalDateTime.of(dato, baseline.plusSeconds(Math.abs(System.nanoTime()) % spread));
        var taskdata = ProsessTaskData.forProsessTask(OpprettGjenopptaTask.class);
        taskdata.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        taskdata.setCallId(callId + "_" + behandling.getId());
        taskdata.setNesteKjøringEtter(nesteKjøring);
        taskTjeneste.lagre(taskdata);
    }


    public String gjenopplivBehandlinger() {
        var callId = ensureCallId();
        LOG.info("BATCH Gjenoppliv inngang");
        var feileteBehandlinger = behandlingProsesseringTjeneste.behandlingerMedFeiletProsessTask();
        var sovende = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt().stream()
            .filter(b -> !feileteBehandlinger.contains(b.getId()))
            .toList();
        var dato = LocalDate.now();
        var baseline = LocalTime.now();
        LOG.info("BATCH Gjenoppliv fant {} behandlinger", sovende.size());
        for (var behandling : sovende) {
            opprettProsessTasks(behandling, callId, dato, baseline, 101);
        }
        LOG.info("BATCH Gjenoppliv utgang");
        return "-" + sovende.size();
    }

    private String ensureCallId() {
        if (MDCOperations.getCallId() == null) {
            MDCOperations.putCallId();
        }
        return MDCOperations.getCallId();
    }
}
