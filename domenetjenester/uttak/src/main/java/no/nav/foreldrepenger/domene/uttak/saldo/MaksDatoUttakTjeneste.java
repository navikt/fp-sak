package no.nav.foreldrepenger.domene.uttak.saldo;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

import java.time.LocalDate;
import java.util.Optional;

public interface MaksDatoUttakTjeneste {

    Optional<LocalDate> beregnMaksDatoUttak(UttakInput uttakInput);

    default Optional<LocalDate> beregnMaksDatoUttakSakskompleks(UttakInput uttakInput, int restStønadsDager) {
        return beregnMaksDatoUttak(uttakInput);
    }
}
