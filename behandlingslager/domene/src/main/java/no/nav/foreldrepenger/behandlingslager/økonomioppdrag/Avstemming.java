package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Denne klassen er en ren avbildning fra Oppdragsløsningens meldingsformater.
 * Den sikrer at avstemmingsnøkkel er alltid i riktig format som tilsvarer: yyyy-MM-dd-HH.mm.ss.SSS */
@Embeddable
public class Avstemming implements Comparable<Avstemming> {

    private static final String PATTERN = "yyyy-MM-dd-HH.mm.ss.SSS";

    @NotNull
    @Column(name = "nokkel_avstemming", nullable = false, updatable = false, length = 30)
    private String nøkkelAvstemming;

    private Avstemming() {
        // for JPA
    }

    private Avstemming(String nøkkelAvstemming) {
        this.nøkkelAvstemming = nøkkelAvstemming;
    }

    public static Avstemming ny() {
        return fra(LocalDateTime.now());
    }

    public static Avstemming fra(LocalDateTime tidspunkt) {
        return new Avstemming(validateAndFormat(tidspunkt));
    }

    public String getNøkkel() {
        return nøkkelAvstemming;
    }

    public String getTidspunkt() {
        return getNøkkel();
    }

    /**
     * Formats the given LocalDateTime into a String with the given pattern yyyy-MM-dd-HH.mm.ss.SSS
     * @param avstemmingTidspunkt - dato og tid som skal brukes i avstemmingsnøkkel.
     * @return riktig formatert string.
     * @throws NullPointerException hvis parameter er null.
     */
    static String validateAndFormat(LocalDateTime avstemmingTidspunkt) {
        Objects.requireNonNull(avstemmingTidspunkt, "avstemmingTidspunkt");
        var dtf = DateTimeFormatter.ofPattern(PATTERN);
        return avstemmingTidspunkt.format(dtf);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Avstemming) o;
        return getNøkkel().equals(that.getNøkkel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNøkkel());
    }

    @Override
    public String toString() {
        return "Avstemming{" +
            "nøkkelAvstemming='" + nøkkelAvstemming + '\'' +
            '}';
    }

    @Override
    public int compareTo(Avstemming o) {
        return this.nøkkelAvstemming.compareTo(o.nøkkelAvstemming);
    }
}
