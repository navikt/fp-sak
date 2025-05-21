package no.nav.foreldrepenger.behandlingslager.behandling.events;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record SakensPersonerEndretEvent(Long fagsakId, Saksnummer saksnummer, Long behandlingId, String årsak) implements BehandlingEvent {

    public SakensPersonerEndretEvent(Behandling behandling, String årsak) {
        this(behandling.getFagsakId(), behandling.getSaksnummer(), behandling.getId(), årsak);
    }

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
