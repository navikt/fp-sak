package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EnumeratedValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;

public enum Alvorlighetsgrad implements DatabaseKode {
    OK("00"),
    OK_MED_MERKNAD("04"),
    FEIL("08")
    ;

    private static final Map<String, Alvorlighetsgrad> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @EnumeratedValue
    private final String kode;

    Alvorlighetsgrad(String kode) {
        this.kode = kode;
    }

    public static Alvorlighetsgrad fraKode(String kode) {
        Objects.requireNonNull(kode, "alvorlighetsgrad");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Alvorlighetsgrad: " + kode);
        }
        return ad;
    }

    public String getKode() {
        return kode;
    }
}
