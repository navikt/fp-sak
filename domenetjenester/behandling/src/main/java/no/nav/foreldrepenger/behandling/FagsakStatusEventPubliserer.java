package no.nav.foreldrepenger.behandling;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void fireEvent(Fagsak fagsak, Long behandlingId, FagsakStatus gammelStatusIn, FagsakStatus nyStatusIn) {
        if (Objects.equals(gammelStatusIn, nyStatusIn)) {
            return;
        }
        if (gammelStatusIn == null) {
            LOG.info("Fagsak status opprettet: id [{}]; type [{}];", fagsak.getId(), fagsak.getYtelseType());
        } else {
            var gammelStatus = gammelStatusIn.getKode();
            var nyStatus = Optional.ofNullable(nyStatusIn).map(FagsakStatus::getKode).orElse("null");

            if (behandlingId != null) {
                LOG.info("Fagsak status oppdatert: {} -> {}; fagsakId [{}] behandlingId [{}]", gammelStatus, nyStatus, fagsak.getId(), behandlingId);
            } else {
                LOG.info("Fagsak status oppdatert: {} -> {}; fagsakId [{}]", gammelStatus, nyStatus, fagsak.getId()); //$NON-NLS-1$
            }
        }

        var event = new FagsakStatusEvent(fagsak.getId(), fagsak.getAktørId(), gammelStatusIn, nyStatusIn);
        fagsakStatusEvent.fire(event);
    }

    public void fireEvent(Fagsak fagsak, FagsakStatus status) {
        fireEvent(fagsak, null, null, status);
    }
}
