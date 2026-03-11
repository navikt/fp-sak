package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum MedlemskapType implements Kodeverdi, DatabaseKode {

    ENDELIG("ENDELIG", "Endelig"),
    FORELOPIG("FORELOPIG", "Foreløpig"),
    UNDER_AVKLARING("AVKLARES", "Under avklaring"),
    ;

    private static final Map<String, MedlemskapType> KODER = new LinkedHashMap<>();

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

    MedlemskapType(String kode, String navn) {
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
