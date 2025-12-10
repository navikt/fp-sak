package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Fagsystem implements Kodeverdi, MedOffisiellKode {

    FPSAK("FPSAK", "Vedtaksløsning Foreldrepenger", "FS36"),
    TPS("TPS", "TPS", "FS03"),
    JOARK("JOARK", "Joark", "AS36"),
    INFOTRYGD("INFOTRYGD", "Infotrygd", "IT01"),
    ARENA("ARENA", "Arena", "AO01"),
    KELVIN("KELVIN", "Kelvin", "KELVIN"),
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
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert", null),
    ;

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

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Fagsystem, String> {
        @Override
        public String convertToDatabaseColumn(Fagsystem attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Fagsystem convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

    }
}
