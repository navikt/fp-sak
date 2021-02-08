package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum TypeSats implements Kodeverdi {
    DAGLIG("DAG"),
    ENGANG("ENG")
    ;

    private static final String KODEVERK = "TYPE_SATS_TYPE";
    private static final Map<String, TypeSats> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    private String kode;

    TypeSats(String kode) {
        this.kode = kode;
    }

    public static TypeSats fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent TypeSats: " + kode);
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<TypeSats, String> {
        @Override
        public String convertToDatabaseColumn(TypeSats attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public TypeSats convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
