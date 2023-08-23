package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingEnhetEvent;

import java.lang.annotation.Annotation;


@ApplicationScoped
public class BehandlingEnhetEventPubliserer {

    private BeanManager beanManager;

    BehandlingEnhetEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingEnhetEventPubliserer(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    public void fireEvent(Behandling behandling) {
        if (beanManager == null) {
            return;
        }
        var event = new BehandlingEnhetEvent(behandling);
        beanManager.fireEvent(event, new Annotation[] {});
    }
}
