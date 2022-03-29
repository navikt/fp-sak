package no.nav.foreldrepenger.domene.modell;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum BeregningsgrunnlagAndeltype implements Kodeverdi {
    BRUKERS_ANDEL("BRUKERS_ANDEL", "Brukers andel"),
    EGEN_NÆRING("EGEN_NÆRING", "Egen næring"),
    FRILANS("FRILANS", "Frilans"),
    UDEFINERT("-", "Ikke definert"),
    ;
    private static final Map<String, BeregningsgrunnlagAndeltype> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEREGNINGSGRUNNLAG_ANDELTYPE";

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

    BeregningsgrunnlagAndeltype(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, BeregningsgrunnlagAndeltype> kodeMap() {
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

}
