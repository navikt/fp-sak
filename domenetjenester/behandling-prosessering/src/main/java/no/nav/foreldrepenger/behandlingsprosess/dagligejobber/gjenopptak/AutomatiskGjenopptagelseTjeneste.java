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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class AutomatiskGjenopptagelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(AutomatiskGjenopptagelseTjeneste.class);
    private static final Set<OppgaveÅrsak> OPPGAVE_TYPER = Set.of(OppgaveÅrsak.BEHANDLE_SAK, OppgaveÅrsak.REVURDER);

    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public AutomatiskGjenopptagelseTjeneste(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                            OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                            BehandlingRepository behandlingRepository,
                                            BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
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
            opprettProsessTasks(behandling, callId, baseline, 1439);
        }

        return  "-" + behandlingListe.size();
    }

    private void opprettProsessTasks(Behandling behandling, String callId, LocalTime baseline, int spread) {
        var nesteKjøring = LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % spread));
        behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, callId + behandling.getId(), nesteKjøring);
    }


    public String oppdaterBehandlingerFraOppgaveFrist() {
        var tom = LocalDate.now().minusDays(1);
        var fom = DayOfWeek.MONDAY.equals(tom.getDayOfWeek()) ? tom.minusDays(2) : tom;
        var oppgaveListe = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(fom, tom, OPPGAVE_TYPER);
        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        var baseline = LocalTime.now();

        for (var oppgave : oppgaveListe) {
            var behandling = behandlingRepository.hentBehandling(oppgave.getBehandlingId());
            if (!behandling.erSaksbehandlingAvsluttet() && !behandling.isBehandlingPåVent() && behandling.erYtelseBehandling()) {
                opprettProsessTasks(behandling, callId, baseline, 1439);
            }
        }

        return "-" + oppgaveListe.size();
    }

    public String gjenopplivBehandlinger() {
        var sovende = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();
        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";
        var baseline = LocalTime.now();

        for (var behandling : sovende) {
            opprettProsessTasks(behandling, callId, baseline, 101);
        }

        return "-" + sovende.size();
    }
}
