package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum ForretningshendelseType implements Kodeverdi {

    INGEN_HENDELSE("INGEN_HENDELSE", "Ingen hendelse"),
    FØDSEL("FØDSEL", "Fødsel"),
    DØD("DØD", "Død"),
    DØDFØDSEL("DØDFØDSEL", "Dødfødsel"),
    UTFLYTTING("UTFLYTTING", "Utflytting"),

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, ForretningshendelseType> KODER = new LinkedHashMap<>();

    private String navn;
    @JsonValue
    private String kode;

    ForretningshendelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static ForretningshendelseType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ForretningshendelseType: " + kode);
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

}
