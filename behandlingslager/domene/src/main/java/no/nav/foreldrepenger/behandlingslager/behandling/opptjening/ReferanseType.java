package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum ReferanseType implements Kodeverdi {

    ORG_NR("ORG_NR", "Orgnr"),
    AKTØR_ID("AKTØR_ID", "Aktør Id"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    private static final Map<String, ReferanseType> KODER = new LinkedHashMap<>();

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

    ReferanseType(String kode, String navn) {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<ReferanseType, String> {
        @Override
        public String convertToDatabaseColumn(ReferanseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public ReferanseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static ReferanseType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent ReferanseType: " + kode);
            }
            return ad;
        }
    }
}
