package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.util.Objects;

public class YtelseVerdi {

    private Satsen sats;
    private Utbetalingsgrad utbetalingsgrad;

    public YtelseVerdi(Satsen sats) {
        Objects.requireNonNull(sats);
        this.sats = sats;
    }

    public YtelseVerdi(Satsen sats, Utbetalingsgrad utbetalingsgrad) {
        this(sats);
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public Satsen getSats() {
        return sats;
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (YtelseVerdi) o;
        return Objects.equals(sats, that.sats) &&
            Objects.equals(utbetalingsgrad, that.utbetalingsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sats, utbetalingsgrad);
    }
}
