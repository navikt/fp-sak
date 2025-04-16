package no.nav.foreldrepenger.behandlingslager.behandling.events;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record SakensPersonerEndretEvent(Long fagsakId, Saksnummer saksnummer, Long behandlingId) implements BehandlingEvent {

    @Override
    public Long getFagsakId() {
        return fagsakId();
    }

    @Override
    public Saksnummer getSaksnummer() {
        return saksnummer();
    }

    @Override
    public Long getBehandlingId() {
        return behandlingId();
    }

}
