package no.nav.foreldrepenger.produksjonsstyring.fagsakstatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;

/**
 * Observerer og propagerer / håndterer events internt i Behandlingskontroll
 */
@ApplicationScoped
public class FagsakStatusEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(FagsakStatusEventObserver.class);

    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private FagsakRepository fagsakRepository;

    FagsakStatusEventObserver() {
        // For CDI
    }

    @Inject
    public FagsakStatusEventObserver(OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste,
                                     FagsakRepository fagsakRepository) {
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());
        var fagsak = fagsakRepository.finnEksaktFagsak(event.getSaksnummer());
        oppdaterFagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(fagsak, event.getBehandlingId(), event.getNyStatus());
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        if (BehandlingStatus.AVSLUTTET.equals(event.getNyStatus())) {
            LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());
            var fagsak = fagsakRepository.finnEksaktFagsak(event.getSaksnummer());
            oppdaterFagsakStatusTjeneste.lagBehandlingAvsluttetTask(fagsak, event.getBehandlingId());
        } else {
            throw new IllegalStateException(String.format("Utviklerfeil: AvsluttetEvent for behandlingId %s med status %s. Det skal ikke skje og må følges opp",
                event.getBehandlingId(), event.getNyStatus()));
        }
    }
}
