package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum HistorikkAktør implements Kodeverdi {

    BESLUTTER("BESL", "Beslutter"),
    SAKSBEHANDLER("SBH", "Saksbehandler"),
    SØKER("SOKER", "Søker"),
    ARBEIDSGIVER("ARBEIDSGIVER", "Arbeidsgiver"),
    VEDTAKSLØSNINGEN("VL", "Vedtaksløsningen"),
    ;

    private static final Map<String, HistorikkAktør> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @EnumeratedValue
    @JsonValue
    private final String kode;

    HistorikkAktør(String kode, String navn) {
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
