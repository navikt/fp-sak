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
        LOG.info("BATCH Gjenoppta inngang");
        var behandlingListe = behandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse();
        var baseline = LocalTime.now();
        LOG.info("BATCH Gjenoppta fant {} behandlinger", behandlingListe.size());
        var counter = 0;
        for (var behandling : behandlingListe) {
            opprettProsessTasks(behandling, baseline, 1439);
            if (++counter > 500) break;
        }
        LOG.info("BATCH Gjenoppta utgang");
        return  "-" + behandlingListe.size();
    }

    private void opprettProsessTasks(Behandling behandling, LocalTime baseline, int spread) {
        var callId = MDCOperations.getCallId();
        if (callId == null) {
            MDCOperations.putCallId();
            callId = MDCOperations.getCallId();
        }
        MDCOperations.putCallId(callId + "_" + behandling.getId());
        var nesteKjøring = LocalDateTime.of(LocalDate.now(), baseline.plusSeconds(LocalDateTime.now().getNano() % spread));
        behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsettBatch(behandling, nesteKjøring);
        MDCOperations.putCallId(callId);
    }


    public String oppdaterBehandlingerFraOppgaveFrist() {
        LOG.info("BATCH Oppdater inngang");
        var tom = LocalDate.now().minusDays(1);
        var fom = DayOfWeek.MONDAY.equals(tom.getDayOfWeek()) ? tom.minusDays(2) : tom;
        var oppgaveListe = oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(fom, tom, OPPGAVE_TYPER);
        var baseline = LocalTime.now();
        LOG.info("BATCH Oppdater fant {} oppgaver", oppgaveListe.size());
        for (var oppgave : oppgaveListe) {
            var behandling = behandlingRepository.hentBehandlingReadOnly(oppgave.getBehandlingId());
            if (!behandling.erSaksbehandlingAvsluttet() && !behandling.isBehandlingPåVent() && behandling.erYtelseBehandling()) {
                opprettProsessTasks(behandling, baseline, 1439);
            }
        }
        LOG.info("BATCH Oppdater utgang");
        return "-" + oppgaveListe.size();
    }

    public String gjenopplivBehandlinger() {
        LOG.info("BATCH Gjenoppliv inngang");
        var sovende = behandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt();
        var baseline = LocalTime.now();
        LOG.info("BATCH Gjenoppliv fant {} behandlinger", sovende.size());
        for (var behandling : sovende) {
            opprettProsessTasks(behandling, baseline, 101);
        }
        LOG.info("BATCH Gjenoppliv utgang");
        return "-" + sovende.size();
    }
}
