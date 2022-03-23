package no.nav.foreldrepenger.behandling;

import java.util.Optional;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record BehandlendeFagsystem(BehandlendeSystem behandlendeSystem, Saksnummer saksnummer) {

    public BehandlendeFagsystem(BehandlendeSystem behandlendeSystem) {
        this(behandlendeSystem, null);
    }

    public Optional<Saksnummer> getSaksnummer() {
        return Optional.ofNullable(saksnummer);
    }

    public enum BehandlendeSystem {
        VEDTAKSLØSNING,
        MANUELL_VURDERING
    }
}
