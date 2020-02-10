package no.nav.foreldrepenger.familiehendelse.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamiliehendelseEvent;


@ApplicationScoped
public class FamiliehendelseEventPubliserer {

    private Event<FamiliehendelseEvent> familiehendelseEvent;

    FamiliehendelseEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public FamiliehendelseEventPubliserer(Event<FamiliehendelseEvent> familiehendelseEvent) {
        this.familiehendelseEvent = familiehendelseEvent;
    }

    public void fireEvent(FamiliehendelseEvent.EventType eventType, Behandling behandling) {
        FamiliehendelseEvent event = new FamiliehendelseEvent(eventType, behandling.getAktørId(),behandling.getFagsakId(),behandling.getId());
        familiehendelseEvent.fire(event);
    }
}

