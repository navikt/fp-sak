package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class FamiliehendelseEvent  implements BehandlingEvent {

    private EventType eventType;
    private Long fagsakId;
    private Long behandlingId;
    private AktørId aktørId;


    public FamiliehendelseEvent(EventType eventType,AktørId aktørId,Long fagsakId,Long behandlingId) {
        this.eventType = eventType;
        this.aktørId = aktørId;
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;

    }


    @Override
    public Long getFagsakId() {
        return fagsakId;
    }


    @Override
    public AktørId getAktørId() {
        return aktørId;
    }


    @Override
    public Long getBehandlingId() {
        return behandlingId;
    }

    public EventType getEventType(){
        return eventType;
    }
    public enum EventType {
        TERMIN_TIL_FØDSEL
    }
}
