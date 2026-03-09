package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum ForretningshendelseType implements Kodeverdi {

    // Verdier i kode er gitt av no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto subklasser
    FØDSEL("FØDSEL", "Fødsel"),
    DØD("DØD", "Død"),
    DØDFØDSEL("DØDFØDSEL", "Dødfødsel"),
    UTFLYTTING("UTFLYTTING", "Utflytting"),
    // Brukes kun for logging inntil videre. Bør revurdere løpende saker
    FALSKID("FALSKID", "Falsk identitet"),
    // Ikke tatt i bruk
    IDENTIFIKATOR("IDENTIFIKATOR", "Folkeregisteridentifikator"),
    ADRESSEBESKYTTELSE("ADRESSEBESKYTTELSE", "Adressebeskyttelse")

    ;

    private static final Map<String, ForretningshendelseType> KODER = new LinkedHashMap<>();

    private final String navn;
    @JsonValue
    private final String kode;

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
