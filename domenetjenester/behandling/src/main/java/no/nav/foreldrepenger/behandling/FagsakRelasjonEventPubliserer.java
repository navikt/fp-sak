package no.nav.foreldrepenger.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;

@ApplicationScoped
public class FagsakRelasjonEventPubliserer {
    public static final FagsakRelasjonEventPubliserer NULL_EVENT_PUB = new FagsakRelasjonEventPubliserer();
    private Event<FagsakRelasjonEvent> fagsakRelasjonEvent;

    FagsakRelasjonEventPubliserer() {
        // for CDI
    }

    @Inject
    public FagsakRelasjonEventPubliserer(Event<FagsakRelasjonEvent> fagsakRelasjonEvent) {
        this.fagsakRelasjonEvent = fagsakRelasjonEvent;
    }

    public void fireEvent(FagsakRelasjon fagsakRelasjon) {

        if (fagsakRelasjonEvent == null) {
            return;
        }

        var event = new FagsakRelasjonEvent(fagsakRelasjon);
        fagsakRelasjonEvent.fire(event);
    }
}
