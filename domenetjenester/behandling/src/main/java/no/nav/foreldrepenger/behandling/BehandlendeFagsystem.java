package no.nav.foreldrepenger.behandling;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.util.Optional;

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
