package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VedtakResultatType implements Kodeverdi, DatabaseKode {

    INNVILGET("INNVILGET", "Innvilget"),
    AVSLAG("AVSLAG", "Avslag"),
    OPPHØR("OPPHØR", "Opphør"),
    VEDTAK_I_KLAGEBEHANDLING("VEDTAK_I_KLAGEBEHANDLING", "vedtak i klagebehandling"),
    VEDTAK_I_ANKEBEHANDLING("VEDTAK_I_ANKEBEHANDLING", "vedtak i ankebehandling"),
    VEDTAK_I_INNSYNBEHANDLING("VEDTAK_I_INNSYNBEHANDLING", "vedtak i innsynbehandling"),

    ;

    private static final Map<String, VedtakResultatType> KODER = new LinkedHashMap<>();

    private final String navn;

    @JsonValue
    @EnumeratedValue
    private final String kode;

    VedtakResultatType(String kode, String navn) {
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
