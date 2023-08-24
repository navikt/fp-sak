package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class Trekkdager {

    public static final Trekkdager ZERO = new Trekkdager(BigDecimal.ZERO);

    @Column(name = "trekkdager_desimaler", nullable = false)
    private BigDecimal verdi;

    Trekkdager() {
        //hibernate
    }

    public Trekkdager(BigDecimal verdi) {
        this.verdi = verdi.setScale(1, RoundingMode.DOWN);
    }

    public Trekkdager(int verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public BigDecimal decimalValue() {
        return verdi.setScale(1, RoundingMode.DOWN);
    }

    @Override
    public String toString() {
        return verdi.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Trekkdager) o;
        return Objects.equals(decimalValue(), that.decimalValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(decimalValue());
    }

    public boolean merEnn0() {
        return decimalValue().compareTo(BigDecimal.ZERO) > 0;
    }

    public int compareTo(Trekkdager trekkdager) {
        return decimalValue().compareTo(trekkdager.decimalValue());
    }

    public Trekkdager add(Trekkdager trekkdager) {
        return new Trekkdager(decimalValue().add(trekkdager.decimalValue()));
    }
}
