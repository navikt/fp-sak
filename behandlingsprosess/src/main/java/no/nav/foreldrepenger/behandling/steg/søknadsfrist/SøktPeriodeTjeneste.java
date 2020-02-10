package no.nav.foreldrepenger.behandling.steg.søknadsfrist;

import java.util.Optional;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public interface SøktPeriodeTjeneste {

    Optional<LocalDateInterval> finnSøktPeriode(UttakInput input);

}
