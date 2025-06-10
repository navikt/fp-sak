package no.nav.foreldrepenger.behandlingslager.behandling.events;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record BehandlingHenlagtEvent(Saksnummer saksnummer, Long fagsakId, Long behandlingId, FagsakYtelseType ytelseType,
                                     BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) implements BehandlingEvent {

    public BehandlingHenlagtEvent(Behandling behandling, BehandlingResultatType behandlingResultatType) {
        this(behandling.getSaksnummer(), behandling.getFagsakId(), behandling.getId(),
            behandling.getFagsakYtelseType(), behandling.getType(), behandlingResultatType);
    }

    public BehandlingHenlagtEvent {
        if (!behandlingResultatType.erHenlagt()) {
            throw new IllegalArgumentException("BehandlingResultatType " + behandlingResultatType().getNavn() + " er ikke en henleggelse");
        }
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
