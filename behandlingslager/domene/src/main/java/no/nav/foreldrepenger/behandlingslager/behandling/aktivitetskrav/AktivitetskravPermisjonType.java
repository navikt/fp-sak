package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum AktivitetskravPermisjonType implements Kodeverdi {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    UTDANNING("UTDANNING", "Utdanning"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    PERMITTERING("PERMITTERING", "Permittering"),
    ANNEN_PERMISJON("ANNEN_PERMISJON", "Annen permisjon"),
    ;

    private static final Map<String, AktivitetskravPermisjonType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "AKTIVITETSKRAV_PERMISJON_TYPE";

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

    AktivitetskravPermisjonType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static AktivitetskravPermisjonType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AktivitetskravPermisjonType: " + kode);
        }
        return ad;
    }

    public static Map<String, AktivitetskravPermisjonType> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AktivitetskravPermisjonType, String> {
        @Override
        public String convertToDatabaseColumn(AktivitetskravPermisjonType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AktivitetskravPermisjonType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static AktivitetskravPermisjonType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent AktivitetskravPermisjonType: " + kode);
            }
            return ad;
        }
    }
}
