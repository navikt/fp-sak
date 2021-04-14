package no.nav.foreldrepenger.domene.vedtak.batch;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;

/**
 * Observerer og propagerer / håndterer events internt i Behandlingskontroll
 */
@ApplicationScoped
public class FagsakStatusEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(FagsakStatusEventObserver.class);

    private Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser;
    private BehandlingRepository behandlingRepository;

    FagsakStatusEventObserver() {
        // For CDI
    }

    @Inject
    public FagsakStatusEventObserver(@Any Instance<OppdaterFagsakStatus> oppdaterFagsakStatuser, BehandlingRepository behandlingRepository) {
        this.oppdaterFagsakStatuser = oppdaterFagsakStatuser;
        this.behandlingRepository = behandlingRepository;
    }

    public void observerBehandlingOpprettetEvent(@Observes BehandlingStatusEvent.BehandlingOpprettetEvent event) {
        LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());//NOSONAR
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        var oppdaterFagsakStatus = FagsakYtelseTypeRef.Lookup.find(oppdaterFagsakStatuser, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()));
        oppdaterFagsakStatus.oppdaterFagsakNårBehandlingEndret(behandling);
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        LOG.debug("Oppdaterer status på Fagsak etter endring i behandling {}", event.getBehandlingId());//NOSONAR
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        var oppdaterFagsakStatus = FagsakYtelseTypeRef.Lookup.find(oppdaterFagsakStatuser, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + behandling.getFagsakYtelseType().getKode()));
        oppdaterFagsakStatus.oppdaterFagsakNårBehandlingEndret(behandling);
    }
}
