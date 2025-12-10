package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Region implements Kodeverdi {

    /**
     * Konstanter for å skrive ned kodeverdi.
     */
    NORDEN("NORDEN", "Nordisk", 1),
    EOS("EOS", "EU/EØS", 2),
    TREDJELANDS_BORGER("ANNET", "3.landsborger", 3),
    UDEFINERT(STANDARDKODE_UDEFINERT, "3.landsborger", 9),
    ;

    public static final Comparator<Region> COMPARATOR = Comparator.comparing(Region::getRank);

    private static final Map<String, Region> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    @JsonValue
    private String kode;

    private int rank;

    Region(String kode, String navn, int rank) {
        this.kode = kode;
        this.navn = navn;
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

}
