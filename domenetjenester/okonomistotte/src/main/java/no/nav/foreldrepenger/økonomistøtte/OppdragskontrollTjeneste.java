package no.nav.foreldrepenger.økonomistøtte;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;

public interface OppdragskontrollTjeneste {

    Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId);

    Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId, boolean brukFellesEndringstidspunkt);

    void lagre(Oppdragskontroll oppdragskontroll);
}
