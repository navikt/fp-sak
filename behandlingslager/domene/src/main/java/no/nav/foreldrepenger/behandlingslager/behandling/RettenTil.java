package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum RettenTil implements Kodeverdi {

    HAR_RETT_TIL_FP("HAR_RETT_TIL_FP", "Bruker har rett til foreldrepenger"),
    HAR_IKKE_RETT_TIL_FP("HAR_IKKE_RETT_TIL_FP", "Bruker har ikke rett til foreldrepenger"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    private static final Map<String, RettenTil> KODER = new LinkedHashMap<>();

    private String navn;

    @JsonValue
    private String kode;

    RettenTil(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<RettenTil, String> {
        @Override
        public String convertToDatabaseColumn(RettenTil attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public RettenTil convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static RettenTil fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent RettenTil: " + kode);
            }
            return ad;
        }
    }

}
