package no.nav.foreldrepenger.økonomistøtte;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;

public interface OppdragskontrollTjeneste {

    Optional<Oppdragskontroll> opprettOppdrag(OppdragInput input);

    Optional<Oppdragskontroll> simulerOppdrag(OppdragInput input);

    void lagre(Oppdragskontroll oppdragskontroll);
}
