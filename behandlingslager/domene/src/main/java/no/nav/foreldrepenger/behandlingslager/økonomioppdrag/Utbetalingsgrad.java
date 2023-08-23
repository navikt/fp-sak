package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Denne klassen er en ren avbildning fra Oppdragsløsningens meldingsformater.
 * Den sikrer at grad er alltid i riktig format som tilsvarer: Heltall fra 0 - 100.
 */
@Embeddable
public class Utbetalingsgrad {

    public static final Utbetalingsgrad _100 = Utbetalingsgrad.prosent(100);

    @Min(0)
    @Max(100)
    @Digits(integer = 3, fraction = 0)
    @Column(name = "utbetalingsgrad")
    private Integer utebetalingsgrad;

    private Utbetalingsgrad() {
        // for JPA
    }

    private Utbetalingsgrad(Integer grad) {
        this.utebetalingsgrad = grad;
    }

    public static Utbetalingsgrad prosent(int grad) {
        return new Utbetalingsgrad(validate(grad));
    }

    public static Utbetalingsgrad prosent(BigDecimal grad) {
        return Utbetalingsgrad.prosent(scale(grad).intValue());
    }

    public Integer getVerdi() {
        return utebetalingsgrad;
    }

    static Integer validate(Integer utbetalingsgrad) {
        Objects.requireNonNull(utbetalingsgrad, "utbetalingsgrad");
        if (utbetalingsgrad < 0 || utbetalingsgrad > 100) {
            throw new IllegalArgumentException("Utbetalingsgrad er utenfor lovlig intervall [0,100]: " + utbetalingsgrad);
        }
        return utbetalingsgrad;
    }

    private static BigDecimal scale(BigDecimal verdi) {
        Objects.requireNonNull(verdi, "utbetalingsgrad");
        return verdi.setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var utbetalingsgrad1 = (Utbetalingsgrad) o;
        return Objects.equals(getVerdi(), utbetalingsgrad1.getVerdi());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVerdi());
    }

    @Override
    public String toString() {
        return "Grad{" +
            "utbetalingsgrad=" + utebetalingsgrad +
            '}';
    }
}
