package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum InntektspostType implements Kodeverdi {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    LØNN("LØNN", "Lønn"),
    YTELSE("YTELSE", "Ytelse"),
    VANLIG("VANLIG", "Vanlig"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    NÆRING_FISKE_FANGST_FAMBARNEHAGE("NÆRING_FISKE_FANGST_FAMBARNEHAGE", "Jordbruk/Skogbruk/Fiske/FamilieBarnehage"),
    ;

    private static final Map<String, InntektspostType> KODER = new LinkedHashMap<>();

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

    InntektspostType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static InntektspostType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent InntektspostType: " + kode);
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
