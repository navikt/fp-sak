package no.nav.foreldrepenger.behandlingslager.etterkontroll;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KontrollType implements Kodeverdi, DatabaseKode {

    MANGLENDE_FØDSEL("MANGLENDE_FØDSEL", "Kontroll manglende fødsel"),

    ;

    private static final Map<String, KontrollType> KODER = new LinkedHashMap<>();

    private final String navn;

    @JsonValue
    @EnumeratedValue
    private final String kode;

    KontrollType(String kode, String navn) {
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


}
