package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingEnhetEvent;


@ApplicationScoped
public class BehandlingEnhetEventPubliserer {

    private Event<BehandlingEnhetEvent> eventHandler;

    BehandlingEnhetEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingEnhetEventPubliserer(Event<BehandlingEnhetEvent> behandlingEnhetEvent) {
        this.eventHandler = behandlingEnhetEvent;
    }

    public void fireEvent(Behandling behandling) {
        if (eventHandler == null || behandling == null) {
            return;
        }
        var event = new BehandlingEnhetEvent(behandling);
        eventHandler.fire(event);
    }
}
