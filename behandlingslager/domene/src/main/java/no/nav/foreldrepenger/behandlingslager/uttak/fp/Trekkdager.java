package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Trekkdager implements Comparable<Trekkdager> {

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
        return compareTo(ZERO) > 0;
    }

    public boolean mindreEnn0() {
        return compareTo(ZERO) < 0;
    }

    @Override
    public int compareTo(Trekkdager trekkdager) {
        return decimalValue().compareTo(trekkdager.decimalValue());
    }

    public Trekkdager add(Trekkdager trekkdager) {
        return new Trekkdager(decimalValue().add(trekkdager.decimalValue()));
    }

    public Trekkdager subtract(Trekkdager trekkdager) {
        return new Trekkdager(decimalValue().subtract(trekkdager.decimalValue()));
    }
}
