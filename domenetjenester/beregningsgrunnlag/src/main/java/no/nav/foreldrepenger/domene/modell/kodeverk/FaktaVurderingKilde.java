package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum FaktaVurderingKilde implements Kodeverdi {

    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler"),
    KALKULATOR("KALKULATOR", "Kalkulator"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Uspesifisert"),
    ;

    private static final Map<String, FaktaVurderingKilde> KODER = new LinkedHashMap<>();

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

    FaktaVurderingKilde(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static FaktaVurderingKilde fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FaktaVurderingKilde: " + kode);
        }
        return ad;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }
}
