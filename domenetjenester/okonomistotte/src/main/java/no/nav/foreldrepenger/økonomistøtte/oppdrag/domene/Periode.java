package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.time.LocalDate;
import java.util.Objects;

public record Periode(LocalDate fom, LocalDate tom) {

    public Periode {
        Objects.requireNonNull(fom, "Fra-og-med-dato må være satt");
        Objects.requireNonNull(tom, "Til-og-med-dato må være satt");
        if (tom.isBefore(fom)) {
            throw new IllegalArgumentException("Til-og-med-dato før fra-og-med-dato: " + fom + ">" + tom);
        }
    }

    public static Periode of(LocalDate fom, LocalDate tom) {
        return new Periode(fom, tom);
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public boolean overlapper(LocalDate dato) {
        return !dato.isBefore(fom) && !dato.isAfter(tom);
    }

    public boolean omslutter(Periode periode) {
        return !periode.getFom().isBefore(fom) && !periode.getTom().isAfter(tom);
    }

    public static LocalDate max(LocalDate en, LocalDate to) {
        return en.isAfter(to) ? en : to;
    }

    public static LocalDate min(LocalDate en, LocalDate to) {
        return en.isBefore(to) ? en : to;
    }

}
