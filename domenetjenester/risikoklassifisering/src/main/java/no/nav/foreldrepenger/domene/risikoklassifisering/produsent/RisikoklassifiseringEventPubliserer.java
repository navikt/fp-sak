package no.nav.foreldrepenger.domene.risikoklassifisering.produsent;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.RisikoklassifiseringEvent;

/**
 * Håndterer fyring av events via CDI når det skjer en overgang i Behandlingskontroll mellom steg, eller statuser
 */
@ApplicationScoped
public class RisikoklassifiseringEventPubliserer {

    private BeanManager beanManager;

    RisikoklassifiseringEventPubliserer() {
        // null ctor, publiserer ingen events
    }

    @Inject
    public RisikoklassifiseringEventPubliserer(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /** Fyrer event via BeanManager slik at håndtering av events som subklasser andre events blir korrekt. */
    private void doFireEvent(BehandlingEvent event) {
        if (beanManager == null) {
            return;
        }
        beanManager.fireEvent(event, new Annotation[] {});
    }

    public void fireEvent(RisikoklassifiseringEvent event) {
        doFireEvent(event);
    }
}
