package no.nav.foreldrepenger.ny.domene;

import java.util.Objects;

public class YtelseVerdi {

    private Sats sats;
    private Utbetalingsgrad utbetalingsgrad;

    public YtelseVerdi(Sats sats) {
        Objects.requireNonNull(sats);
        this.sats = sats;
    }

    public YtelseVerdi(Sats sats, Utbetalingsgrad utbetalingsgrad) {
        this(sats);
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public Sats getSats() {
        return sats;
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YtelseVerdi that = (YtelseVerdi) o;
        return Objects.equals(sats, that.sats) &&
            Objects.equals(utbetalingsgrad, that.utbetalingsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sats, utbetalingsgrad);
    }
}
