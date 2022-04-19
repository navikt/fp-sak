package no.nav.foreldrepenger.økonomistøtte.oppdrag.domene;

import java.util.Objects;

public class DelytelseId implements Comparable<DelytelseId> {

    private FagsystemId fagsystemId;
    private int løpenummer;
    private boolean gammeltFormat;

    public static DelytelseId opprett(FagsystemId fagsystemId, int teller) {
        var id = new DelytelseId();
        id.fagsystemId = fagsystemId;
        id.løpenummer = teller;
        id.gammeltFormat = false;
        return id;
    }

    public static DelytelseId førsteForFagsystemId(FagsystemId fagsystemId) {
        var id = new DelytelseId();
        id.fagsystemId = fagsystemId;
        id.løpenummer = 100; //TODO start på 1
        id.gammeltFormat = false;
        return id;
    }

    public static DelytelseId parse(String kode) {
        return kode.contains("-")
            ? parseNyttFormat(kode)
            : parseGammeltFormat(kode);
    }

    private static DelytelseId parseNyttFormat(String kode) {
        var index = kode.lastIndexOf('-');
        var id = new DelytelseId();
        id.fagsystemId = FagsystemId.parse(kode.substring(0, index));
        id.løpenummer = Integer.parseInt(kode.substring(index + 1));
        id.gammeltFormat = false;
        return id;
    }

    private static DelytelseId parseGammeltFormat(String kode) {
        var id = new DelytelseId();
        id.fagsystemId = FagsystemId.parse(kode.substring(0, kode.length() - 3));
        id.løpenummer = Integer.parseInt(kode.substring(kode.length() - 3));
        id.gammeltFormat = true;
        return id;
    }

    private DelytelseId() {
    }

    String getSaksnummer() {
        return fagsystemId.getSaksnummer();
    }

    int getFagsystemIdLøpenummer() {
        return fagsystemId.getLøpenummer();
    }

    int getLøpenummer() {
        return løpenummer;
    }

    boolean isGammeltFormat() {
        return gammeltFormat;
    }

    public DelytelseId neste() {
        return increment(1);
    }

    public FagsystemId getFagsystemId() {
        return fagsystemId;
    }

    public DelytelseId increment(int add) {
        var id = new DelytelseId();
        id.fagsystemId = this.fagsystemId;
        id.løpenummer = this.løpenummer + add;
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
        var that = (DelytelseId) annen;
        return løpenummer == that.løpenummer &&
            fagsystemId.equals(that.fagsystemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsystemId, løpenummer);
    }

    @Override
    public String toString() {
       /* return gammeltFormat
            ? String.format("%s%03d", fagsystemId, løpenummer)
            : String.format("%s-%d", fagsystemId, løpenummer);*/
        return String.format("%s%03d", fagsystemId, løpenummer);
    }

    @Override
    public int compareTo(DelytelseId o) {
        var resultat = fagsystemId.compareTo(o.fagsystemId);
        if (resultat != 0) {
            return resultat;
        }
        return Long.compare(løpenummer, o.løpenummer);
    }
}
