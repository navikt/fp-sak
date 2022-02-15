package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.time.LocalDate;

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

    public LocalDate getFraVerdi() {
        return fraVerdi;
    }

    public LocalDate getTilVerdi() {
        return tilVerdi;
    }

}
