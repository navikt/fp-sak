package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Embeddable
public class MorsStillingsprosent implements Comparable<MorsStillingsprosent> {

    @JsonValue
    @Column(name = "mors_stillingsprosent")
    @DecimalMin(value = "0.00", inclusive = false)
    @DecimalMax(value = "75.00", inclusive = false)
    @Digits(integer = 2, fraction = 2)
    private BigDecimal verdi;

    @JsonCreator
    public MorsStillingsprosent(BigDecimal verdi) {
        if (verdi == null || verdi.compareTo(BigDecimal.valueOf(75)) >= 0 || verdi.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mors stillingsprosent har en ugyldig verdi: " + verdi);
        }
        this.verdi = scale(verdi);
    }

    public MorsStillingsprosent() {

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
        var that = (MorsStillingsprosent) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }

    @Override
    public int compareTo(MorsStillingsprosent morsStillingsprosent) {
        return decimalValue().compareTo(morsStillingsprosent.decimalValue());
    }


}
