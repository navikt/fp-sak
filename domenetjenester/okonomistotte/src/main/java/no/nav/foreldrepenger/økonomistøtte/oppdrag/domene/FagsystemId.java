package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.util.Objects;

public class FagsystemId implements Comparable<FagsystemId> {

    private String saksnummer;
    private int løpenummer;
    private boolean gammeltFormat;

    public static FagsystemId førsteForFagsak(String saksnummer) {
        var id = new FagsystemId();
        id.saksnummer = saksnummer;
        id.løpenummer = 100;
        id.gammeltFormat = false;
        return id;
    }

    public static FagsystemId parse(String kode) {
        return kode.contains("-")
            ? parseNyttFormat(kode)
            : parseGammeltFormat(kode);
    }

    private static FagsystemId parseNyttFormat(String kode) {
        var index = kode.lastIndexOf('-');

        var id = new FagsystemId();
        id.saksnummer = kode.substring(0, index);
        id.løpenummer = Integer.parseInt(kode.substring(index + 1));
        id.gammeltFormat = false;
        return id;
    }

    private static FagsystemId parseGammeltFormat(String kode) {
        var id = new FagsystemId();
        id.saksnummer = kode.substring(0, kode.length() - 3);
        id.løpenummer = Integer.parseInt(kode.substring(kode.length() - 3));
        id.gammeltFormat = true;
        return id;
    }

    private FagsystemId() {
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    int getLøpenummer() {
        return løpenummer;
    }

    boolean isGammeltFormat() {
        return gammeltFormat;
    }

    public FagsystemId neste() {
        var id = new FagsystemId();
        id.saksnummer = this.saksnummer;
        id.løpenummer = this.løpenummer + 1;
        id.gammeltFormat = this.gammeltFormat; //TODO alltid sette til true ?
        return id;
    }

    @Override
    public boolean equals(Object annen) {
        if (this == annen) {
            return true;
        }
        if (annen == null || getClass() != annen.getClass()) {
            return false;
        }
        var that = (FagsystemId) annen;
        return løpenummer == that.løpenummer &&
            saksnummer.equals(that.saksnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saksnummer, løpenummer);
    }

    @Override
    public String toString() {
        return String.format("%s%03d", saksnummer, løpenummer);
        /*return gammeltFormat
            ? String.format("%s%03d", saksnummer, løpenummer)
            : String.format("%s-%d", saksnummer, løpenummer);*/
    }

    @Override
    public int compareTo(FagsystemId o) {
        var resultat = saksnummer.compareTo(o.saksnummer);
        if (resultat != 0) {
            return resultat;
        }
        return Long.compare(løpenummer, o.løpenummer);
    }
}
