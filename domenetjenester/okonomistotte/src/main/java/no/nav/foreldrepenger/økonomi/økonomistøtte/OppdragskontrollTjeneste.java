package no.nav.foreldrepenger.økonomi.økonomistøtte;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

public interface OppdragskontrollTjeneste {

    Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId);

    void lagre(Oppdragskontroll oppdragskontroll);
}
