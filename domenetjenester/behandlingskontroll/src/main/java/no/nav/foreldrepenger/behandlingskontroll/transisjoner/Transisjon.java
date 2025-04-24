package no.nav.foreldrepenger.behandlingskontroll.transisjoner;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public record Transisjon(StegTransisjon stegTransisjon, BehandlingStegType målSteg) {

    public Transisjon(StegTransisjon transisjon) {
        this(transisjon, null);
    }

    public Transisjon {
        if (stegTransisjon == null) {
            throw new IllegalArgumentException("Utviklerfeil: stegTransisjon kan ikke være null");
        }
        if (stegTransisjon.direkteTilGittDestinasjon() && målSteg == null) {
            throw new IllegalArgumentException("Utviklerfeil: stegTransisjon må ha målsteg");
        }
        if (!stegTransisjon.direkteTilGittDestinasjon() && målSteg != null) {
            throw new IllegalArgumentException("Utviklerfeil: stegTransisjon skal ikke ha målsteg");
        }
    }

    public Optional<BehandlingStegType> getMålSteg() {
        return Optional.ofNullable(målSteg());
    }

    public BehandlingStegModell nesteSteg(BehandlingStegModell nåværendeSteg) {
        return switch (stegTransisjon()) {
            case STARTET, RETURNER -> throw new IllegalStateException("Utviklerfeil: skal ikke kalle nesteSteg for stegTransisjon " + stegTransisjon().name());
            case SUSPENDERT -> nåværendeSteg;
            case UTFØRT -> nåværendeSteg.getNesteSteg();
            case HENLEGG -> null;
            case HOPPOVER, FLYOVER -> nåværendeSteg.getSenereStegHvisFinnes(målSteg());
        };
    }

}
