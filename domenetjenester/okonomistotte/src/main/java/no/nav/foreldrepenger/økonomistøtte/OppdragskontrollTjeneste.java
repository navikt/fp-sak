package no.nav.foreldrepenger.økonomistøtte;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;

public interface OppdragskontrollTjeneste {

    Optional<Oppdragskontroll> opprettOppdrag(Input input);

    Optional<Oppdragskontroll> simulerOppdrag(Input input);

    Optional<Oppdragskontroll> opprettOppdrag(Input input, boolean brukFellesEndringstidspunkt);

    @Deprecated
    Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId);

    @Deprecated
    Optional<Oppdragskontroll> simulerOppdrag(Long behandlingId);

    @Deprecated
    Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId, boolean brukFellesEndringstidspunkt);

    void lagre(Oppdragskontroll oppdragskontroll);
}
