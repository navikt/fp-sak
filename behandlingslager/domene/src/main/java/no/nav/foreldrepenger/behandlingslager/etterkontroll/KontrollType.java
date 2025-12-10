package no.nav.foreldrepenger.behandlingslager.etterkontroll;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KontrollType implements Kodeverdi {

    MANGLENDE_FØDSEL("MANGLENDE_FØDSEL", "Kontroll manglende fødsel"),

    ;

    private static final Map<String, KontrollType> KODER = new LinkedHashMap<>();

    private final String navn;

    @JsonValue
    private final String kode;

    KontrollType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KontrollType, String> {
        @Override
        public String convertToDatabaseColumn(KontrollType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KontrollType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static KontrollType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent KontrollType: " + kode);
            }
            return ad;
        }
    }

}
