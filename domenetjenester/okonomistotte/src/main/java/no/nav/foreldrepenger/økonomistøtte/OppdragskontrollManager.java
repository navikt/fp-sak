package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;

public interface OppdragskontrollManager {
    Oppdragskontroll opprettØkonomiOppdrag(OppdragInput input, Oppdragskontroll oppdragskontroll);
}
