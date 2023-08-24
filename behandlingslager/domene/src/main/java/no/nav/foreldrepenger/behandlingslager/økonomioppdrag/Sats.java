package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Sats representert i hele kroner.
 */
@Embeddable
public class Sats {

    @Min(1)
    @Digits(integer = 9, fraction = 0)
    @Column(name = "sats")
    private Integer sats;

    protected Sats() {
        // for hibernate
    }

    private Sats(Integer verdi) {
        this.sats = verdi;
    }

    public static Sats på(int verdi) {
        return new Sats(validate(verdi));
    }

    public static Sats på(long verdi) {
        return Sats.på((int) verdi);
    }

    public static Sats på(BigDecimal verdi) {
        return Sats.på(scale(verdi).intValue());
    }

    private static BigDecimal scale(BigDecimal verdi) {
        Objects.requireNonNull(verdi, "sats");
        return verdi.setScale(0, RoundingMode.HALF_UP);
    }

    public Integer getVerdi() {
        return sats;
    }

    static Integer validate(Integer sats) {
        Objects.requireNonNull(sats, "sats");
        if (sats < 0) {
            throw new IllegalArgumentException("Sats er utenfor lovlig intervall 0 - " + Integer.MAX_VALUE + ", var: "  + sats);
        }
        return sats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var sats1 = (Sats) o;
        return Objects.equals(sats, sats1.sats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sats);
    }

    @Override
    public String toString() {
        return "Sats{" +
            "sats=" + sats +
            '}';
    }
}
