package no.nav.foreldrepenger.familiehendelse.event;

import java.time.LocalDate;

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

    public void fireEventTerminFødsel(Behandling behandling, LocalDate forrigeBekreftetDato, LocalDate sisteBekreftetDato) {
        FamiliehendelseEvent event = new FamiliehendelseEvent(FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL,
            behandling.getAktørId(),behandling.getFagsakId(),behandling.getId(), forrigeBekreftetDato, sisteBekreftetDato);
        familiehendelseEvent.fire(event);
    }
}

