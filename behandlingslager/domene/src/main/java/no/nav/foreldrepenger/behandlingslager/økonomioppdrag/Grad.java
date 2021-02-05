package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Denne klassen er en ren avbildning fra Oppdragsløsningens meldingsformater.
 * Den sikrer at grad er alltid i riktig format som tilsvarer: Heltall fra 0 - 100.
 */
@Embeddable
public class Grad {

    public static final Grad _100 = Grad.prosent(100);

    private static final String TYPE_GRAD = "UFOR";

    @Min(0)
    @Max(100)
    @Digits(integer = 3, fraction = 0)
    @Column(name = "grad", updatable = false)
    private Integer grad;

    private Grad() {
        // for JPA
    }

    private Grad(Integer grad) {
        this.grad = grad;
    }

    public static Grad prosent(int grad) {
        return new Grad(validate(grad));
    }

    public static Grad prosent(BigDecimal grad) {
        return Grad.prosent(validate(scale(grad).intValue()));
    }

    public Integer getVerdi() {
        return grad;
    }

    public String getType() {
        return TYPE_GRAD;
    }

    static Integer validate(Integer grad) {
        Objects.requireNonNull(grad, "grad");
        if (grad < 0 || grad > 100) {
            throw new IllegalArgumentException("Grad er utenfor lovlig intervall [0,100]: " + grad);
        }
        return grad;
    }

    private static BigDecimal scale(BigDecimal verdi) {
        Objects.requireNonNull(verdi, "grad");
        return verdi.setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grad grad1 = (Grad) o;
        return Objects.equals(getVerdi(), grad1.getVerdi());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVerdi());
    }

    @Override
    public String toString() {
        return "Grad{" +
            "grad=" + grad +
            '}';
    }
}
