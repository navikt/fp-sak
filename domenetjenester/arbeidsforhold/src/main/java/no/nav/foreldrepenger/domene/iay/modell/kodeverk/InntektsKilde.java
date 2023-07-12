package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum InntektsKilde implements Kodeverdi, MedOffisiellKode {

    UDEFINERT("-", "Ikke definert", null),
    INNTEKT_OPPTJENING("INNTEKT_OPPTJENING", "INNTEKT_OPPTJENING", "INNTEKT"),
    INNTEKT_BEREGNING("INNTEKT_BEREGNING", "INNTEKT_BEREGNING", null),
    INNTEKT_SAMMENLIGNING("INNTEKT_SAMMENLIGNING", "INNTEKT_SAMMENLIGNING", null),
    SIGRUN("SIGRUN", "Sigrun", "SIGRUN"),
    VANLIG("VANLIG", "Vanlig", "VANLIG"),
    ;

    private static final Map<String, InntektsKilde> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "INNTEKTS_KILDE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;
    @JsonValue
    private final String kode;
    private final String offisiellKode;

    InntektsKilde(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static InntektsKilde fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent InntektsKilde: " + kode);
        }
        return ad;
    }

    public static Map<String, InntektsKilde> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
