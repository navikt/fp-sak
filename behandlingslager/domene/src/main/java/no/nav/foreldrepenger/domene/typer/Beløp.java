package no.nav.foreldrepenger.domene.typer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseValue;

/**
 * Beløp representerer kombinasjon av kroner og øre på standardisert format
 */
@Embeddable
public class Beløp implements Serializable, IndexKey, TraverseValue {
    public static final Beløp ZERO = new Beløp(BigDecimal.ZERO);

    @Column(name = "beloep", scale = 2)
    @ChangeTracked
    private BigDecimal verdi;

    protected Beløp() {
        // for hibernate
    }

    public static Beløp av(long verdi) {
        return new Beløp((int) verdi);
    }

    public static Beløp fra(BigDecimal beløp) {
        return beløp != null ? new Beløp(beløp) : null;
    }


    public Beløp(BigDecimal verdi) {
        this.verdi = verdi;
    }

    // Beleilig å kunne opprette gjennom int
    public Beløp(Integer verdi) {
        this.verdi = verdi == null ? null : new BigDecimal(verdi);
    }

    private BigDecimal skalertVerdi() {
        return verdi == null ? null : verdi.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getIndexKey() {
        var skalertVerdi = skalertVerdi();
        return skalertVerdi != null ? skalertVerdi.toString() : null;
    }

    public BigDecimal getVerdi() {
        return verdi;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        var other = (Beløp) obj;
        return Objects.equals(skalertVerdi(), other.skalertVerdi());
    }

    @Override
    public int hashCode() {
        return Objects.hash(skalertVerdi());
    }

    @Override
    public String toString() {
        return "Beløp{" + "verdi=" + verdi + ", skalertVerdi=" + skalertVerdi() + '}';
    }

    public int compareTo(Beløp annetBeløp) {
        return verdi.compareTo(annetBeløp.getVerdi());
    }

    public boolean erNullEllerNulltall() {
        return verdi == null || erNulltall();
    }

    public boolean erNulltall() {
        return verdi != null && compareTo(Beløp.ZERO) == 0;
    }

    public Beløp multipliser(int multiplicand) {
        return new Beløp(this.verdi.multiply(BigDecimal.valueOf(multiplicand)));
    }

    public Beløp adder(Beløp augend) {
        return new Beløp(this.verdi.add(augend.getVerdi()));
    }

    public Beløp subtract(Beløp verdi) {
        return new Beløp(this.verdi.subtract(verdi.getVerdi()));
    }
}
