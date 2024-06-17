package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Barn som inngår i foreldrepengergrunnlaget for ytelsen.
 */
public class Barn {
    private final LocalDate dødsdato;

    public Barn(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    public Barn() {
        this(null);
    }

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
    }
}
