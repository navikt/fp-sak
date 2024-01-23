package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Fagsystem implements Kodeverdi, MedOffisiellKode {

    FPSAK("FPSAK", "Vedtaksløsning Foreldrepenger", "FS36"),
    TPS("TPS", "TPS", "FS03"),
    JOARK("JOARK", "Joark", "AS36"),
    INFOTRYGD("INFOTRYGD", "Infotrygd", "IT01"),
    ARENA("ARENA", "Arena", "AO01"),
    INNTEKT("INNTEKT", "INNTEKT", "FS28"),
    MEDL("MEDL", "MEDL", "FS18"),
    GOSYS("GOSYS", "Gosys", "FS22"),
    ENHETSREGISTERET("ENHETSREGISTERET", "Enhetsregisteret", "ER01"),
    AAREGISTERET("AAREGISTERET", "AAregisteret", "AR01"),
    K9SAK("K9SAK", "Vedtaksløsning Folketrygdloven Kapittel 9", "K9SAK"),
    VLSP("VLSP", "Vedtaksløsning Sykepenger", "VLSP"),

    /**
     * Alle kodeverk må ha en verdi, det kan ikke være null i databasen. Denne koden gjør samme nytten.
     */
    UDEFINERT("-", "Ikke definert", null),
    ;

    public static final String KODEVERK = "FAGSYSTEM";

    private static final Map<String, Fagsystem> KODER = new LinkedHashMap<>();

    private final String navn;

    private final String offisiellKode;
    @JsonValue
    private final String kode;

    Fagsystem(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static Fagsystem fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Fagsystem: " + kode);
        }
        return ad;
    }

    public static Map<String, Fagsystem> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
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
