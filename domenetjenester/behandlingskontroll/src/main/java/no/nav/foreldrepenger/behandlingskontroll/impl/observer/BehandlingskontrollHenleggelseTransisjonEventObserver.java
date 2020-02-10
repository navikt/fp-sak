package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;

@ApplicationScoped
class BehandlingskontrollHenleggelseTransisjonEventObserver {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    BehandlingskontrollHenleggelseTransisjonEventObserver() {
        //for CDI proxy
    }

    @Inject
    public BehandlingskontrollHenleggelseTransisjonEventObserver(BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public void observerBehandlingSteg(@Observes BehandlingTransisjonEvent event) {

        if (FellesTransisjoner.HENLAGT.equals(event.getTransisjonIdentifikator())) {
            behandlingskontrollTjeneste.henleggBehandlingFraSteg(event.getKontekst(), BehandlingResultatType.HENLAGT_SØKNAD_MANGLER);

        }
    }
}



