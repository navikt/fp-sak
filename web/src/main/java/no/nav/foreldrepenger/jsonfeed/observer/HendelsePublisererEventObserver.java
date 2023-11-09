package no.nav.foreldrepenger.jsonfeed.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.jsonfeed.HendelsePublisererTjeneste;

@ApplicationScoped
public class HendelsePublisererEventObserver {

    private HendelsePublisererTjeneste hendelsePubliserere;

    public HendelsePublisererEventObserver() {
        //Classic Design Institute
    }

    @Inject
    public HendelsePublisererEventObserver(HendelsePublisererTjeneste hendelsePubliserere) {
        this.hendelsePubliserere = hendelsePubliserere;
    }

    public void observerBehandlingVedtak(@Observes BehandlingVedtakEvent event) {
        hendelsePubliserere.lagreVedtak(event.vedtak());
    }
}
