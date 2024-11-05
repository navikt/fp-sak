package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Optional;

public interface Uttak {
    Optional<LocalDate> opphørsdato();

    boolean harUlikUttaksplan(Uttak uttak);

    boolean harUlikKontoEllerMinsterett(Uttak uttak);

    default boolean erOpphør() {
        return opphørsdato().isPresent();
    }

    boolean harOpphørsUttakNyeInnvilgetePerioder(Uttak uttak);

    boolean harAvslagPgaMedlemskap();

    boolean altAvslått();
}
