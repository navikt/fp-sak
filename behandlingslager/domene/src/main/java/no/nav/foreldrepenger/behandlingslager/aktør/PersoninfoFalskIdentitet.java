package no.nav.foreldrepenger.behandlingslager.aktør;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public record PersoninfoFalskIdentitet(String navn,
                                       LocalDate fødselsdato,
                                       NavBrukerKjønn kjønn,
                                       Landkoder statsborgerskap,
                                       PersonstatusType personstatus) {
    public PersoninfoFalskIdentitet {
        // Kan mangle fødselsdato
        Objects.requireNonNull(navn, "Falsk identitet skal ha navn");
        Objects.requireNonNull(kjønn, "Falsk identitet skal ha kjønn");
        Objects.requireNonNull(statsborgerskap, "Falsk identitet skal ha statsborgerskap");
        Objects.requireNonNull(personstatus, "Falsk identitet skal ha personstatus");
    }

}
