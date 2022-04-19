package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.util.Objects;

public class Satsen {
    private final SatsType satsType;
    private final long sats;

    public static Satsen dagsats(long sats) {
        return new Satsen(SatsType.DAG, sats);
    }
    public static Satsen engang(long sats) {
        return new Satsen(SatsType.ENG, sats);
    }

    public Satsen(SatsType satsType, long sats) {
        this.satsType = satsType;
        this.sats = sats;
    }

    public SatsType getSatsType() {
        return satsType;
    }

    public long getSats() {
        return sats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var sats1 = (Satsen) o;
        return sats == sats1.sats &&
            satsType == sats1.satsType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(satsType, sats);
    }

    @Override
    public String toString() {
        return "Sats{" +
            "satsType=" + satsType +
            ", sats=" + sats +
            '}';
    }
}
