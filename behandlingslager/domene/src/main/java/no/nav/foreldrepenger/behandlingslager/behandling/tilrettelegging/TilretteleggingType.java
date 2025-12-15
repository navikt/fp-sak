package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum TilretteleggingType implements Kodeverdi {

    HEL_TILRETTELEGGING("HEL_TILRETTELEGGING", "Hel tilrettelegging"),
    DELVIS_TILRETTELEGGING("DELVIS_TILRETTELEGGING", "Delvis tilrettelegging"),
    INGEN_TILRETTELEGGING("INGEN_TILRETTELEGGING", "Ingen tilrettelegging"),
    ;

    private static final Map<String, TilretteleggingType> KODER = new LinkedHashMap<>();

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

    TilretteleggingType(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<TilretteleggingType, String> {
        @Override
        public String convertToDatabaseColumn(TilretteleggingType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public TilretteleggingType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static TilretteleggingType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent TilretteleggingType: " + kode);
            }
            return ad;
        }
    }
}
