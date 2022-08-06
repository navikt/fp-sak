package no.nav.foreldrepenger.domene.uttak.søknadsfrist;

import java.util.Optional;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public interface SøktPeriodeTjeneste {

    Optional<LocalDateInterval> finnSøktPeriode(UttakInput input);

}
