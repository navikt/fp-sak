package no.nav.foreldrepenger.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

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
                LOG.info("Fagsak status oppdatert: {} -> {}; fagsakId [{}]", gammelStatus, nyStatus, fagsak.getId());
            }
        }

        var event = new FagsakStatusEvent(fagsak.getId(), behandlingId, fagsak.getAktørId(), gammelStatusIn, nyStatusIn);
        fagsakStatusEvent.fire(event);
    }

    public void fireEvent(Fagsak fagsak, FagsakStatus status) {
        fireEvent(fagsak, null, null, status);
    }
}
