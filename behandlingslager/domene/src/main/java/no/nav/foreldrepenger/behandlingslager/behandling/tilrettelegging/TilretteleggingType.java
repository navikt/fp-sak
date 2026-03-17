package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum TilretteleggingType implements Kodeverdi, DatabaseKode {

    HEL_TILRETTELEGGING("HEL_TILRETTELEGGING", "Hel tilrettelegging"),
    DELVIS_TILRETTELEGGING("DELVIS_TILRETTELEGGING", "Delvis tilrettelegging"),
    INGEN_TILRETTELEGGING("INGEN_TILRETTELEGGING", "Ingen tilrettelegging"),
    ;

    private static final Map<String, TilretteleggingType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @JsonValue
    @EnumeratedValue
    private final String kode;

    TilretteleggingType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
