package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Periode {

    private static final DateTimeFormatter DATO_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Column(name = "fom")
    private LocalDate fom;

    @Column(name = "tom")
    private LocalDate tom;

    private Periode() {
    }

    public Periode(LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(fom, "Fra-og-med-dato må være satt");
        Objects.requireNonNull(tom, "Til-og-med-dato må være satt");
        if (tom.isBefore(fom)) {
            throw new IllegalArgumentException("Til-og-med-dato før fra-og-med-dato: " + fom + ">" + tom);
        }
        this.fom = fom;
        this.tom = tom;
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

    @Override
    public String toString() {
        return fom.format(DATO_FORMAT) + "-" + tom.format(DATO_FORMAT);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Periode) {
            return Objects.equals(getFom(), ((Periode) o).getFom())
                && Objects.equals(getTom(), ((Periode) o).getTom());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom);
    }

}
