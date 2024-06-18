package no.nav.foreldrepenger.behandlingslager.behandling.events;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.AktørId;

public record BehandlingSaksbehandlerEvent(Long fagsakId, Long behandlingId, AktørId aktørId) implements BehandlingEvent {

    public BehandlingSaksbehandlerEvent(Behandling behandling) {
        this(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId());
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

}
