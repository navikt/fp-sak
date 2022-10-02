package no.nav.foreldrepenger.jsonfeed.observer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
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
