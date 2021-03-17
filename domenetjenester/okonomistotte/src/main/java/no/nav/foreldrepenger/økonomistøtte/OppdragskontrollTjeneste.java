package no.nav.foreldrepenger.økonomistøtte;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;

public interface OppdragskontrollTjeneste {

    Optional<Oppdragskontroll> opprettOppdrag(Input input);

    Optional<Oppdragskontroll> simulerOppdrag(Input input);

    @Deprecated
    Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId);

    void lagre(Oppdragskontroll oppdragskontroll);
}
