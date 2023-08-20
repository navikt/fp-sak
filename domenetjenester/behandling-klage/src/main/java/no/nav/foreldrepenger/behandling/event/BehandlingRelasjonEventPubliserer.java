package no.nav.foreldrepenger.behandling.event;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingRelasjonEvent;


@ApplicationScoped
public class BehandlingRelasjonEventPubliserer {

    private BeanManager beanManager;

    BehandlingRelasjonEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public BehandlingRelasjonEventPubliserer(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    public void fireEvent(Behandling behandling) {
        if (beanManager == null) {
            return;
        }
        var event = new BehandlingRelasjonEvent(behandling);
        beanManager.fireEvent(event, new Annotation[] {});
    }
}
