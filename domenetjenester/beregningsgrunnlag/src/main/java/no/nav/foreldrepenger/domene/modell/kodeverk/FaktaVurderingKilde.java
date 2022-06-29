package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.folketrygdloven.kalkulus.kodeverk.Kodeverdi;

public enum FaktaVurderingKilde implements Kodeverdi {

    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler"),
    KALKULATOR("KALKULATOR", "Kalkulator"),
    UDEFINERT("-", "Uspesifisert"),
    ;

    private static final Map<String, FaktaVurderingKilde> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FAKTA_VURDERING_KILDE";

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

    public static Map<String, FaktaVurderingKilde> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }
}
