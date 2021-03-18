package no.nav.foreldrepenger.økonomistøtte;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.OppdragInput;

public interface OppdragskontrollTjeneste {

    Optional<Oppdragskontroll> opprettOppdrag(OppdragInput input);

    Optional<Oppdragskontroll> simulerOppdrag(OppdragInput input);

    @Deprecated
    Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId);

    void lagre(Oppdragskontroll oppdragskontroll);
}
