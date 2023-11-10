package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum MedlemskapType implements Kodeverdi {

    ENDELIG("ENDELIG", "Endelig"),
    FORELOPIG("FORELOPIG", "Foreløpig"),
    UNDER_AVKLARING("AVKLARES", "Under avklaring"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, MedlemskapType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MEDLEMSKAP_TYPE";

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

    MedlemskapType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, MedlemskapType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<MedlemskapType, String> {
        @Override
        public String convertToDatabaseColumn(MedlemskapType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public MedlemskapType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static MedlemskapType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent MedlemskapType: " + kode);
            }
            return ad;
        }
    }
}
