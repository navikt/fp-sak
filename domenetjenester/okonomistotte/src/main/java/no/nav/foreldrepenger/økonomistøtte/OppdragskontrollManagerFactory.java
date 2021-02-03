package no.nav.foreldrepenger.økonomistøtte;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public interface OppdragskontrollManagerFactory {
    Optional<OppdragskontrollManager> getManager(Behandling behandling, boolean tidligereOppdragFinnes);
}
