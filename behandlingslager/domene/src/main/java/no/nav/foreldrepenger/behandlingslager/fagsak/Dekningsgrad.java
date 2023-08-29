package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Dekningsgrad for foreldrepenger.
 */
@Embeddable
public class Dekningsgrad {

    public static final Dekningsgrad _100 = new Dekningsgrad(100);
    public static final Dekningsgrad _80 = new Dekningsgrad(80);

    @Column(name = "dekningsgrad")
    private int verdi;

    @SuppressWarnings("unused")
    Dekningsgrad() {
        // for hibernate
    }

    public Dekningsgrad(int verdi) {
        if (verdi < 0 || verdi > 100) {
            throw new IllegalArgumentException("!( 0 < value < 100 )");
        }
        this.verdi = verdi;
    }

    public static Dekningsgrad grad(int verdi) {
        return new Dekningsgrad(verdi);
    }

    public int getVerdi() {
        return verdi;
    }

    @Override
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof Dekningsgrad other)) {
            return false;
        }
        if (arg0 == this) {
            return true;
        }

        return Objects.equals(verdi, other.verdi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(verdi);
    }

    public boolean isÅtti() {
        return this.equals(_80);
    }
}
