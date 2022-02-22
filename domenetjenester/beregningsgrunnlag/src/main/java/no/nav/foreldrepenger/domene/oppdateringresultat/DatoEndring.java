package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.time.LocalDate;
import java.util.Optional;

public class DatoEndring {

    private LocalDate fraVerdi;

    private LocalDate tilVerdi;

    public DatoEndring() {
        // For Json deserialisering
    }

    public DatoEndring(LocalDate fraVerdi, LocalDate tilVerdi) {
        this.fraVerdi = fraVerdi;
        this.tilVerdi = tilVerdi;
    }

    public boolean erEndret() {
        return tilVerdi != null && (fraVerdi == null || !tilVerdi.equals(fraVerdi));
    }

    public Optional<LocalDate> getFraVerdi() {
        return Optional.ofNullable(fraVerdi);
    }

    public LocalDate getTilVerdi() {
        return tilVerdi;
    }

}
