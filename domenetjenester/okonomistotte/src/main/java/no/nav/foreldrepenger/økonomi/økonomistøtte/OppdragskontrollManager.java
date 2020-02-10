package no.nav.foreldrepenger.økonomi.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

public interface OppdragskontrollManager {
    Oppdragskontroll opprettØkonomiOppdrag(Behandling behandling, Oppdragskontroll oppdragskontroll);
}
