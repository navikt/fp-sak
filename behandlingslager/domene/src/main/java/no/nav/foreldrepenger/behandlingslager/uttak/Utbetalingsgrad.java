package no.nav.foreldrepenger.behandlingslager.uttak;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Embeddable
public class Utbetalingsgrad implements Comparable<Utbetalingsgrad> {

    public static final Utbetalingsgrad ZERO = new Utbetalingsgrad(0);
    public static final Utbetalingsgrad TEN = new Utbetalingsgrad(10);
    public static final Utbetalingsgrad FULL = new Utbetalingsgrad(100);
    public static final Utbetalingsgrad HUNDRED = FULL;

    @JsonValue
    @Column(name = "utbetalingsprosent")
    @Min(0)
    @Max(100)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal verdi;

    @JsonCreator
    public Utbetalingsgrad(BigDecimal verdi) {
        this.verdi = scale(verdi);
    }

    public Utbetalingsgrad(double verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public Utbetalingsgrad(int verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    Utbetalingsgrad() {

    }

    public boolean harUtbetaling() {
        return compareTo(Utbetalingsgrad.ZERO) > 0;
    }

    public boolean erRedusert() {
        return harUtbetaling() && !erFull();
    }

    public boolean erFull() {
        return compareTo(Utbetalingsgrad.FULL) >= 0;
    }

    public BigDecimal decimalValue() {
        return scale(verdi);
    }

    private BigDecimal scale(BigDecimal verdi) {
        return verdi.setScale(2, RoundingMode.UP);
    }

    @Override
    public String toString() {
        return verdi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Utbetalingsgrad) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }

    @Override
    public int compareTo(Utbetalingsgrad utbetalingsgrad) {
        return decimalValue().compareTo(utbetalingsgrad.decimalValue());
    }
}
