package no.nav.foreldrepenger.domene.risikoklassifisering.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.risikoklassifisering.Risikoklassifisering;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.RisikoklassifiseringEvent;

@ApplicationScoped
public class RisikoklassifiseringEventObserver {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private Risikoklassifisering risikoklassifisering;

    public RisikoklassifiseringEventObserver() {
    }

    @Inject
    public RisikoklassifiseringEventObserver(Risikoklassifisering risikoklassifisering) {
        this.risikoklassifisering = risikoklassifisering;
    }

    public void observerRisikoklassifiseringKjøringEvent(@Observes RisikoklassifiseringEvent event) {
        try {
            risikoklassifisering.opprettProsesstaskForRisikovurdering(event.getBehandlingRef());
        } catch (Exception ex) {
            log.warn("Publisering av RisikoklassifiseringsTask feilet", ex);
        }
    }
}
