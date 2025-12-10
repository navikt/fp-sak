package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum InntektsmeldingInnsendingsårsak implements Kodeverdi {

    NY("NY", "NY"),
    ENDRING("ENDRING", "ENDRING"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "UDEFINERT"),
    ;

    private static final Map<String, InntektsmeldingInnsendingsårsak> KODER = new LinkedHashMap<>();

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

    InntektsmeldingInnsendingsårsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static InntektsmeldingInnsendingsårsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent InntektsmeldingInnsendingsårsak: " + kode);
        }
        return ad;
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
