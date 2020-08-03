package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AutomatiskGjenopptagelseTjeneste.class);
    private static final Set<OppgaveÅrsak> OPPGAVE_TYPER = Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER);

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                            BehandlingRepository behandlingRepository,
                                            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public AutomatiskGjenopptagelseTjeneste() {
        // for CDI
    }

    public String gjenopptaBehandlinger() {
        List<Behandling> behandlingListe = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();
        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        LocalTime baseline = LocalTime.now();

        for (Behandling behandling : behandlingListe) {
            String nyCallId = callId + behandling.getId();
            opprettProsessTask(behandling, nyCallId, baseline, 1439);
        }

        //TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return "-";
    }

    private void opprettProsessTask(Behandling behandling, String callId, LocalTime baseline, int spread) {
        log.info("oppretter task med ny callId: {} ", callId);
        ProsessTaskData prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % spread)));

        // unik per task da det gjelder  ulike behandlinger, gjenbruker derfor ikke
        prosessTaskData.setCallId(callId);

        prosessTaskRepository.lagre(prosessTaskData);
    }

    public List<TaskStatus> hentStatusForGjenopptaBehandlingGruppe(String gruppe) {

        return prosessTaskRepository.finnStatusForTaskIGruppe(GjenopptaBehandlingTask.TASKTYPE, gruppe);
    }

    public String oppdaterBehandlingerFraOppgaveFrist() {
        LocalDate tom = LocalDate.now().minusDays(1);
        LocalDate fom = DayOfWeek.MONDAY.equals(tom.getDayOfWeek()) ? tom.minusDays(2) : tom;
        List<OppgaveBehandlingKobling> oppgaveListe = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(fom, tom, OPPGAVE_TYPER);
        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        LocalTime baseline = LocalTime.now();

        for (OppgaveBehandlingKobling oppgave : oppgaveListe) {
            Behandling behandling = behandlingRepository.hentBehandling(oppgave.getBehandlingId());
            if (!behandling.erSaksbehandlingAvsluttet() && !behandling.isBehandlingPåVent() && behandling.erYtelseBehandling()) {
                String nyCallId = callId + behandling.getId();
                opprettProsessTask(behandling, nyCallId, baseline, 1439);
            }
        }

        //TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return "-";
    }

    public String gjenopplivBehandlinger() {
        List<Behandling> sovende = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();
        String callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        LocalTime baseline = LocalTime.now();

        for (Behandling behandling : sovende) {
            String nyCallId = callId + behandling.getId();
            opprettProsessTask(behandling, nyCallId, baseline, 101);
        }

        //TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return "-";
    }
}
