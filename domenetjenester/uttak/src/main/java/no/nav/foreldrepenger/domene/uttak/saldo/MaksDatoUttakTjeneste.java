package no.nav.foreldrepenger.domene.uttak.saldo;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

import java.time.LocalDate;
import java.util.Optional;

public interface MaksDatoUttakTjeneste {

    public Optional<LocalDate> beregnMaksDatoUttak(UttakInput uttakInput);

}
