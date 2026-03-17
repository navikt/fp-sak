package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum IverksettingStatus implements Kodeverdi, DatabaseKode {

    IKKE_IVERKSATT("IKKE_IVERKSATT", "Ikke iverksatt"),
    IVERKSATT("IVERKSATT", "Iverksatt"),
    ;

    private static final Map<String, IverksettingStatus> KODER = new LinkedHashMap<>();

    private final String navn;

    @JsonValue
    @EnumeratedValue
    private final String kode;

    IverksettingStatus(String kode, String navn) {
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
