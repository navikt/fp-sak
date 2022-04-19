package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.util.Objects;

public class Utbetalingsgrad {

    private int utbetalingsgrad;

    public static Utbetalingsgrad prosent(int utbetalingsgrad) {
        return new Utbetalingsgrad(utbetalingsgrad);
    }

    public Utbetalingsgrad(int utbetalingsgrad) {
        if (utbetalingsgrad < 0 || utbetalingsgrad > 100) {
            throw new IllegalArgumentException("Utbetalingsgrad er utenfor lovlig intervall [1,100]: " + utbetalingsgrad);
        }
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public int getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Utbetalingsgrad) o;
        return utbetalingsgrad == that.utbetalingsgrad;
    }

    @Override
    public int hashCode() {
        return Objects.hash(utbetalingsgrad);
    }

    @Override
    public String toString() {
        return "Utbetalingsgrad{" + utbetalingsgrad + '}';
    }
}
