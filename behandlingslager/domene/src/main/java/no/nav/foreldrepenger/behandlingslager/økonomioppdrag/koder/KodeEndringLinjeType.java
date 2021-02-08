package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KodeEndringLinjeType implements Kodeverdi {
    NY("NY"),
    ENDRING("ENDR")
    ;

    private static final String KODEVERK = "KODE_ENDRING_LINJE_TYPE";
    private static final Map<String, KodeEndringLinjeType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    private String kode;

    KodeEndringLinjeType(String kode) {
        this.kode = kode;
    }

    public static KodeEndringLinjeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KodeEndringLinjeType: " + kode);
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
    public static class KodeverdiConverter implements AttributeConverter<KodeEndringLinjeType, String> {
        @Override
        public String convertToDatabaseColumn(KodeEndringLinjeType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KodeEndringLinjeType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
