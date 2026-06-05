package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Optional;

public interface Uttak {
    Optional<LocalDate> opphørsdato();

    boolean erEndretUttaksplanFra(Uttak originaltUttak);

    boolean harUlikKontoEllerMinsterett(Uttak uttak);

    default boolean erOpphør() {
        return opphørsdato().isPresent();
    }

    boolean harOpphørsUttakNyeInnvilgedePerioderFra(Uttak originaltUttak);

    boolean harAvslagPgaMedlemskap();

    boolean altAvslått();
}
