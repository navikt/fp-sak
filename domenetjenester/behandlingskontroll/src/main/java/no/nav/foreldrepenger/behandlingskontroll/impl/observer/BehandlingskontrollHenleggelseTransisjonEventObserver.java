package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;

@ApplicationScoped
class BehandlingskontrollHenleggelseTransisjonEventObserver {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    BehandlingskontrollHenleggelseTransisjonEventObserver() {
        // for CDI proxy
    }

    @Inject
    public BehandlingskontrollHenleggelseTransisjonEventObserver(BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public void observerBehandlingSteg(@Observes BehandlingTransisjonEvent event) {

        if (StegTransisjon.HENLEGG.equals(event.getStegTransisjon())) {
            behandlingskontrollTjeneste.henleggBehandlingFraSteg(event.getKontekst(), BehandlingResultatType.HENLAGT_SØKNAD_MANGLER);

        }
    }
}
