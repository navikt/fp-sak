package no.nav.foreldrepenger.behandlingslager.behandling.innsyn;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum InnsynResultatType implements Kodeverdi, DatabaseKode {

    INNVILGET("INNV", "Innvilget innsyn"),
    DELVIS_INNVILGET("DELV", "Delvis innvilget innsyn"),
    AVVIST("AVVIST", "Avslått innsyn"),
    ;

    private static final Map<String, InnsynResultatType> KODER = new LinkedHashMap<>();

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

    InnsynResultatType(String kode, String navn) {
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
