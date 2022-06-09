package no.nav.foreldrepenger.behandling;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;

@ApplicationScoped
public class FagsakStatusEventPubliserer {
    private static final Logger LOG = LoggerFactory.getLogger(FagsakStatusEventPubliserer.class);

    private Event<FagsakStatusEvent> fagsakStatusEvent;

    FagsakStatusEventPubliserer() {
        // for CDI
    }

    @Inject
    public FagsakStatusEventPubliserer(Event<FagsakStatusEvent> fagsakStatusEvent) {
        this.fagsakStatusEvent = fagsakStatusEvent;
    }

    public void fireEvent(Fagsak fagsak, Behandling behandling, FagsakStatus gammelStatusIn, FagsakStatus nyStatusIn) {
        fireEventBehandlingId(fagsak, behandling != null ? behandling.getId() : null, gammelStatusIn, nyStatusIn);
    }

    public void fireEventBehandlingId(Fagsak fagsak, Long behandlingId, FagsakStatus gammelStatusIn, FagsakStatus nyStatusIn) {
        if (((gammelStatusIn == null) && (nyStatusIn == null)) // NOSONAR
                || Objects.equals(gammelStatusIn, nyStatusIn)) { // NOSONAR
            // gjør ingenting
            return;
        }
        if ((gammelStatusIn == null) && (nyStatusIn != null)) {// NOSONAR
            LOG.info("Fagsak status opprettet: id [{}]; type [{}];", fagsak.getId(), fagsak.getYtelseType());
        } else {
            var fagsakId = fagsak.getId();
            var gammelStatus = gammelStatusIn.getKode(); // NOSONAR false positive NPE dereference
            var nyStatus = nyStatusIn == null ? null : nyStatusIn.getKode();

            if (behandlingId != null) {
                LOG.info("Fagsak status oppdatert: {} -> {}; fagsakId [{}] behandlingId [{}]", gammelStatus, nyStatus, fagsakId, behandlingId);
            } else {
                LOG.info("Fagsak status oppdatert: {} -> {}; fagsakId [{}]", gammelStatus, nyStatus, fagsakId); //$NON-NLS-1$
            }
        }

        var event = new FagsakStatusEvent(fagsak.getId(), fagsak.getAktørId(), gammelStatusIn, nyStatusIn);
        fagsakStatusEvent.fire(event);
    }

    public void fireEvent(Fagsak fagsak, FagsakStatus status) {
        fireEvent(fagsak, null, null, status);
    }
}
