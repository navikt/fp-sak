package no.nav.foreldrepenger.domene.uttak.saldo;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

public interface MaksDatoUttakTjeneste {

    Optional<LocalDate> beregnMaksDatoUttak(UttakInput uttakInput);

}
