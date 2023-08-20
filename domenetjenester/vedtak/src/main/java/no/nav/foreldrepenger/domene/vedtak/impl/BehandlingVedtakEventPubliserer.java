package no.nav.foreldrepenger.domene.vedtak.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

@ApplicationScoped
public class BehandlingVedtakEventPubliserer {

    private Event<BehandlingVedtakEvent> behandlingVedtakEvent;

    BehandlingVedtakEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingVedtakEventPubliserer(Event<BehandlingVedtakEvent> behandlingVedtakEvent) {
        this.behandlingVedtakEvent = behandlingVedtakEvent;
    }

    public void fireEvent(BehandlingVedtak vedtak, Behandling behandling) {

        var event = new BehandlingVedtakEvent(vedtak, behandling);
        behandlingVedtakEvent.fire(event);
    }
}
