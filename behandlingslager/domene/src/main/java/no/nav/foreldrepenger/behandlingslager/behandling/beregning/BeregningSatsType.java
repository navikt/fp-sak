package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum BeregningSatsType implements Kodeverdi, DatabaseKode {
    ENGANG("ENGANG", "Engangsstønad"),
    GRUNNBELØP("GRUNNBELØP", "Grunnbeløp"),
    GSNITT("GSNITT", "Grunnbeløp årsgjennomsnitt"),
    ;

    private static final Map<String, BeregningSatsType> KODER = new LinkedHashMap<>();

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

    BeregningSatsType(String kode, String navn) {
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
