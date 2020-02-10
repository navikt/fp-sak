package no.nav.foreldrepenger.behandlingskontroll.events;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class BehandlingStegTilstandEndringEvent implements BehandlingEvent {

    private BehandlingStegTilstandSnapshot fraTilstand;
    private BehandlingStegTilstandSnapshot tilTilstand;

    private BehandlingskontrollKontekst kontekst;


    public BehandlingStegTilstandEndringEvent(BehandlingskontrollKontekst kontekst, BehandlingStegTilstandSnapshot fraTilstand, BehandlingStegTilstandSnapshot tilTilstand){
        super();
        this.kontekst = kontekst;
        this.fraTilstand = fraTilstand;
        this.tilTilstand = tilTilstand;
    }

    @Override
    public Long getFagsakId() {
        return kontekst.getFagsakId();
    }

    @Override
    public AktørId getAktørId() {
        return kontekst.getAktørId();
    }

    @Override
    public Long getBehandlingId() {
        return kontekst.getBehandlingId();
    }

    public Optional<BehandlingStegTilstandSnapshot> getFraTilstand() {
        return Optional.ofNullable(fraTilstand);
    }

    public Optional<BehandlingStegTilstandSnapshot> getTilTilstand() {
        return Optional.ofNullable(tilTilstand);
    }
}
