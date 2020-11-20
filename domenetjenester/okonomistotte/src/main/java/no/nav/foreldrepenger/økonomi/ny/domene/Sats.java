package no.nav.foreldrepenger.økonomi.ny.domene;

import java.util.Objects;

public class Sats {
    private SatsType satsType;
    private long sats;

    public static Sats dagsats(long sats) {
        return new Sats(SatsType.DAG, sats);
    }

    public static Sats dag7(long sats) {
        return new Sats(SatsType.DAG7, sats);
    }

    public static Sats engang(long sats) {
        return new Sats(SatsType.ENGANG, sats);
    }

    public Sats(SatsType satsType, long sats) {
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
        Sats sats1 = (Sats) o;
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
