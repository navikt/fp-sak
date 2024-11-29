package no.nav.foreldrepenger.behandlingslager.behandling.events;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record BehandlingRelasjonEvent(Saksnummer saksnummer, Long fagsakId, Long behandlingId) implements BehandlingEvent {

    public BehandlingRelasjonEvent(Behandling behandling) {
        this(behandling.getSaksnummer(), behandling.getFagsakId(), behandling.getId());
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

}
