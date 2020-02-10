package no.nav.foreldrepenger.behandling.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class BehandlingEnhetEvent implements BehandlingEvent {
    private Long fagsakId;
    private Long behandlingId;
    private AktørId aktørId;


    public BehandlingEnhetEvent(Behandling behandling) {
        this.fagsakId = behandling.getFagsakId();
        this.behandlingId = behandling.getId();
        this.aktørId = behandling.getAktørId();
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
