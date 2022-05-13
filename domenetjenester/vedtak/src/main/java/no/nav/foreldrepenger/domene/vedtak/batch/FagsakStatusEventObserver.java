package no.nav.foreldrepenger.domene.vedtak.batch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusTjeneste;

/**
 * Observerer og propagerer / håndterer events internt i Behandlingskontroll
 */
@ApplicationScoped
public class FagsakStatusEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(FagsakStatusEventObserver.class);

    private OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste;
    private BehandlingRepository behandlingRepository;

    FagsakStatusEventObserver() {
        // For CDI
    }

    @Inject
    public FagsakStatusEventObserver(OppdaterFagsakStatusTjeneste oppdaterFagsakStatusTjeneste, BehandlingRepository behandlingRepository) {
        this.oppdaterFagsakStatusTjeneste = oppdaterFagsakStatusTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());//NOSONAR
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        oppdaterFagsakStatusTjeneste.oppdaterFagsakNårBehandlingOpprettet(behandling);
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());//NOSONAR
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        oppdaterFagsakStatusTjeneste.oppdaterFagsakNårBehandlingAvsluttet(behandling);
    }
}
