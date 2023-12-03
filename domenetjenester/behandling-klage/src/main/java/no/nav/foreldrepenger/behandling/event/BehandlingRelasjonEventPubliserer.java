package no.nav.foreldrepenger.behandling.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;


@ApplicationScoped
public class BehandlingRelasjonEventPubliserer {

    private Event<BehandlingRelasjonEvent> eventHandler;

    BehandlingRelasjonEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingRelasjonEventPubliserer(Event<BehandlingRelasjonEvent> eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void fireEvent(Behandling behandling) {
        if (eventHandler == null || behandling == null) {
            return;
        }
        var event = new BehandlingRelasjonEvent(behandling);
        eventHandler.fire(event);
    }
}
