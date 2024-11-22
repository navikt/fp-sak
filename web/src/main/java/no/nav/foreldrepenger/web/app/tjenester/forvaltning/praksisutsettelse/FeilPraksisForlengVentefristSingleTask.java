package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import static no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.EnhetsTjeneste.MIDLERTIDIG_ENHET;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@Dependent
@ProsessTask(value = "behandling.ventefristpraksisutsettelse.single", prioritet = 4, maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
class FeilPraksisForlengVentefristSingleTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisForlengVentefristSingleTask.class);
    private static final LocalDateTime FRIST = LocalDate.of(2025, 9, 26).atStartOfDay();
    private static final List<String> OPPGAVE_TYPER = List.of(Oppgavetype.VURDER_DOKUMENT.getKode(), Oppgavetype.VURDER_KONSEKVENS_YTELSE.getKode(),
        "VURD_HENV", "VUR_SVAR", "KONT_BRUK");

    private final BehandlingRepository behandlingRepository;
    private final OppgaveTjeneste oppgaveTjeneste;
    private final AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    @Inject
    FeilPraksisForlengVentefristSingleTask(BehandlingRepository behandlingRepository,
                                           OppgaveTjeneste oppgaveTjeneste,
                                           AksjonspunktKontrollRepository aksjonspunktKontrollRepository) {
        this.behandlingRepository = behandlingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erAvsluttet()) {
            LOG.info("FeilPraksisUtsettelse: Forlenger ikke ventefrist. Behandling er avsluttet");
        } else if (!behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD)) {
            LOG.info("FeilPraksisUtsettelse: Forlenger ikke ventefrist. Mangler vent aksjonspunkt");
        } else if (FRIST.toLocalDate().isBefore(behandling.getFristDatoBehandlingPåVent())) {
            LOG.info("FeilPraksisUtsettelse: Forlenger ikke ventefrist. Frist allerede forlenget");
//        } else if (harÅpenKlageAnke(behandling)) {
//            LOG.info("FeilPraksisUtsettelse: Forlenger ventefrist. Har klage eller anke");
//            forlengVentefrist(behandling);
//        } else if (harOppgaverIGosys(behandling)) {
//            LOG.info("FeilPraksisUtsettelse: Forlenger ventefrist. Fant oppgaver i gosys");
//            forlengVentefrist(behandling);
//        } else {
//            LOG.info("FeilPraksisUtsettelse: Forlenger ikke ventefrist. Bruker ikke vært i kontakt");
        } else {
            LOG.info("FeilPraksisUtsettelse: Forlenger ventefrist");
            forlengVentefrist(behandling);
        }
    }

    private boolean harÅpenKlageAnke(Behandling behandling) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(behandling.getFagsakId())
            .stream()
            .anyMatch(b -> Set.of(BehandlingType.ANKE, BehandlingType.KLAGE).contains(b.getType()));
    }

    private boolean harOppgaverIGosys(Behandling behandling) {
        return !oppgaveTjeneste.hentOppgaver(OPPGAVE_TYPER, behandling.getAktørId().getId(), MIDLERTIDIG_ENHET.enhetId(), "50").isEmpty();
    }

    private void forlengVentefrist(Behandling behandling) {
        var aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD);
        aksjonspunktKontrollRepository.setFrist(behandling, aksjonspunkt, FRIST, aksjonspunkt.getVenteårsak());
    }
}
