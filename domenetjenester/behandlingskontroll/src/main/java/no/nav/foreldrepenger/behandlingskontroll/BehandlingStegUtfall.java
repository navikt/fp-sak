package no.nav.foreldrepenger.behandlingskontroll;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

public record BehandlingStegUtfall(BehandlingStegType behandlingStegType, BehandlingStegStatus resultat) {
}
