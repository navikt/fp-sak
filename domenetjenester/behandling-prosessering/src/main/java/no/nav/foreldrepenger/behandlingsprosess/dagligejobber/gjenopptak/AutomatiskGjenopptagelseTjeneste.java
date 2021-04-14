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
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskGjenopptagelseTjeneste.class);
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
        var behandlingListe = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();
        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        var baseline = LocalTime.now();

        for (var behandling : behandlingListe) {
            var nyCallId = callId + behandling.getId();
            opprettProsessTask(behandling, nyCallId, baseline, 1439);
        }

        // TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en
        // annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return "-";
    }

    private void opprettProsessTask(Behandling behandling, String callId, LocalTime baseline, int spread) {
        LOG.info("oppretter task med ny callId: {} ", callId);
        var prosessTaskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");
        prosessTaskData.setPrioritet(100);
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % spread)));

        // unik per task da det gjelder ulike behandlinger, gjenbruker derfor ikke
        prosessTaskData.setCallId(callId);

        prosessTaskRepository.lagre(prosessTaskData);
    }

    public List<TaskStatus> hentStatusForGjenopptaBehandlingGruppe(String gruppe) {

        return prosessTaskRepository.finnStatusForTaskIGruppe(GjenopptaBehandlingTask.TASKTYPE, gruppe);
    }

    public String oppdaterBehandlingerFraOppgaveFrist() {
        var tom = LocalDate.now().minusDays(1);
        var fom = DayOfWeek.MONDAY.equals(tom.getDayOfWeek()) ? tom.minusDays(2) : tom;
        var oppgaveListe = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(fom, tom,
                OPPGAVE_TYPER);
        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        var baseline = LocalTime.now();

        for (var oppgave : oppgaveListe) {
            var behandling = behandlingRepository.hentBehandling(oppgave.getBehandlingId());
            if (!behandling.erSaksbehandlingAvsluttet() && !behandling.isBehandlingPåVent() && behandling.erYtelseBehandling()) {
                var nyCallId = callId + behandling.getId();
                opprettProsessTask(behandling, nyCallId, baseline, 1439);
            }
        }

        // TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en
        // annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return "-";
    }

    public String gjenopplivBehandlinger() {
        var sovende = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();
        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        var baseline = LocalTime.now();

        for (var behandling : sovende) {
            var nyCallId = callId + behandling.getId();
            opprettProsessTask(behandling, nyCallId, baseline, 101);
        }

        // TODO(OJR) må endres i forbindelsen med at løsningen ser på task_grupper på en
        // annet måte nå, hvis en prosess feiler i en gruppe stopper alt opp..
        return "-";
    }
}
