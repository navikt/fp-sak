package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;

import java.util.Optional;

public interface OppdragskontrollTjeneste {

    Optional<Oppdragskontroll> opprettOppdrag(OppdragInput input);

    Optional<Oppdragskontroll> simulerOppdrag(OppdragInput input);

    void lagre(Oppdragskontroll oppdragskontroll);
}
