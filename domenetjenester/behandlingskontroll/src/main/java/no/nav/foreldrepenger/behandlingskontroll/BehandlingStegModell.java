package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public interface BehandlingStegModell {

    /**
     * Type kode for dette steget.
     */
    BehandlingStegType getBehandlingStegType();

    /**
     * Implementasjon av et gitt steg i behandlingen.
     */
    BehandlingSteg getSteg();

    BehandlingModell getBehandlingModell();

}
