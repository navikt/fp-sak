package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskGjenopptagelseTjeneste.class);
    private static final Set<OppgaveÅrsak> OPPGAVE_TYPER = Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER);

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                            ProsessTaskTjeneste taskTjeneste,
                                            BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
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
        taskdata.setBehandling(behandling.getFagsakId(), behandling.getId());
        taskdata.setCallId(callId + "_" + behandling.getId());
        taskdata.setNesteKjøringEtter(nesteKjøring);
        taskdata.setPrioritet(50);
        taskTjeneste.lagre(taskdata);
    }


    public String oppdaterBehandlingerFraOppgaveFrist() {
        var callId = ensureCallId();
        LOG.info("BATCH Oppdater inngang");
        var tom = LocalDate.now().minusDays(1);
        var fom = DayOfWeek.MONDAY.equals(tom.getDayOfWeek()) ? tom.minusDays(2) : tom;
        var feileteBehandlinger = behandlingProsesseringTjeneste.behandlingerMedFeiletProsessTask();
        var behandlingListe = oppgaveBehandlingKoblingRepository.hentBehandlingerMedUferdigeOppgaverOpprettetTidsrom(fom, tom, OPPGAVE_TYPER);
        var dato = LocalDate.now();
        var baseline = LocalTime.now();
        LOG.info("BATCH Oppdater fant {} oppgaver", behandlingListe.size());
        behandlingListe.stream()
            .filter(b -> !feileteBehandlinger.contains(b.getId()))
            .filter(b -> !b.erSaksbehandlingAvsluttet() && !b.isBehandlingPåVent() && b.erYtelseBehandling())
            .forEach(behandling -> opprettProsessTasks(behandling, callId, dato, baseline, 1439));
        LOG.info("BATCH Oppdater utgang");
        return "-" + behandlingListe.size();
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
