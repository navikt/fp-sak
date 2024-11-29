package no.nav.foreldrepenger.familiehendelse.event;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.events.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;


@ApplicationScoped
public class FamiliehendelseEventPubliserer {

    private Event<FamiliehendelseEvent> familiehendelseEvent;
    private BehandlingRepository behandlingRepository;

    FamiliehendelseEventPubliserer() {
        //Cyclopedia Drainage Invariant
    }

    @Inject
    public FamiliehendelseEventPubliserer(Event<FamiliehendelseEvent> familiehendelseEvent, BehandlingRepository behandlingRepository) {
        this.familiehendelseEvent = familiehendelseEvent;
        this.behandlingRepository = behandlingRepository;
    }

    public void fireEventTerminFødsel(Long behandlingId, LocalDate forrigeBekreftetDato, LocalDate sisteBekreftetDato) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var event = new FamiliehendelseEvent(FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL,
            behandling.getSaksnummer(), behandling.getFagsakId(), behandling.getId(), behandling.getFagsakYtelseType(),
            forrigeBekreftetDato, sisteBekreftetDato);
        familiehendelseEvent.fire(event);
    }
}

