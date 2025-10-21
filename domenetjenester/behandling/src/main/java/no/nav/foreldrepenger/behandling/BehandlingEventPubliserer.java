package no.nav.foreldrepenger.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;

@ApplicationScoped
public class BehandlingEventPubliserer {

    public static final BehandlingEventPubliserer NULL_EVENT_PUB = new BehandlingEventPubliserer();

    private Event<BehandlingEvent> behandlingEvent;

    BehandlingEventPubliserer() {
        // CDI
    }

    @Inject
    public BehandlingEventPubliserer(Event<BehandlingEvent> behandlingEvent) {
        this.behandlingEvent = behandlingEvent;
    }

    public void publiserBehandlingEvent(BehandlingEvent event) {
        if (behandlingEvent != null && event != null) {
            behandlingEvent.fire(event);
        }
    }
}
