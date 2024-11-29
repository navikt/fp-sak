package no.nav.foreldrepenger.behandlingslager.behandling.events;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class FamiliehendelseEvent  implements BehandlingEvent {

    private EventType eventType;
    private Long fagsakId;
    private Long behandlingId;
    private Saksnummer saksnummer;
    private FagsakYtelseType ytelseType;
    private LocalDate forrigeBekreftetDato;
    private LocalDate sisteBekreftetDato;


    public FamiliehendelseEvent(EventType eventType, Saksnummer saksnummer, Long fagsakId, Long behandlingId,
                                FagsakYtelseType ytelseType, LocalDate forrigeBekreftetDato, LocalDate sisteBekreftetDato) {
        this.eventType = eventType;
        this.saksnummer = saksnummer;
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;
        this.ytelseType = ytelseType;
        this.forrigeBekreftetDato = forrigeBekreftetDato;
        this.sisteBekreftetDato = sisteBekreftetDato;
    }


    @Override
    public Long getFagsakId() {
        return fagsakId;
    }


    @Override
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }


    @Override
    public Long getBehandlingId() {
        return behandlingId;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public LocalDate getForrigeBekreftetDato() {
        return forrigeBekreftetDato;
    }

    public LocalDate getSisteBekreftetDato() {
        return sisteBekreftetDato;
    }

    public EventType getEventType(){
        return eventType;
    }
    public enum EventType {
        TERMIN_TIL_FØDSEL
    }
}
