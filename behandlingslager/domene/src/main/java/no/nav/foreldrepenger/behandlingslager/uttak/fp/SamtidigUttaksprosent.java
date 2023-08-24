package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class SamtidigUttaksprosent implements Comparable<SamtidigUttaksprosent> {

    public static final SamtidigUttaksprosent ZERO = new SamtidigUttaksprosent(0);
    public static final SamtidigUttaksprosent TEN = new SamtidigUttaksprosent(10);

    @JsonValue
    @Column(name = "samtidig_uttaksprosent")
    @Min(0)
    @Max(100)
    @Digits(integer = 3, fraction = 2)
    private BigDecimal verdi;

    @JsonCreator
    public SamtidigUttaksprosent(BigDecimal verdi) {
        this.verdi = scale(verdi);
    }

    public SamtidigUttaksprosent(double verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public SamtidigUttaksprosent(int verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    SamtidigUttaksprosent() {

    }

    public boolean merEnn0() {
        return this.verdi.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean erSamtidigUttak(SamtidigUttaksprosent samtidigUttaksprosent) {
        return samtidigUttaksprosent != null && samtidigUttaksprosent.merEnn0();
    }

    public BigDecimal decimalValue() {
        return scale(verdi);
    }

    private BigDecimal scale(BigDecimal verdi) {
        return verdi.setScale(2, RoundingMode.DOWN);
    }

    @Override
    public String toString() {
        return verdi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (SamtidigUttaksprosent) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }

    @Override
    public int compareTo(SamtidigUttaksprosent samtidigUttaksprosent) {
        return decimalValue().compareTo(samtidigUttaksprosent.decimalValue());
    }
}
