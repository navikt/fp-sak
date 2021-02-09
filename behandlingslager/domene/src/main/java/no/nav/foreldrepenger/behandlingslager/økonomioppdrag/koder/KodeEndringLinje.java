package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KodeEndringLinje implements Kodeverdi {
    NY("NY"),
    ENDRING("ENDR")
    ;

    private static final String KODEVERK = "KODE_ENDRING_LINJE_TYPE";
    private static final Map<String, KodeEndringLinje> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;

    KodeEndringLinje(String kode) {
        this.kode = kode;
    }

    public static KodeEndringLinje fraKode(String kode) {
        Objects.requireNonNull(kode, "kodeEndringLinje");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KodeEndringLinjeType: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return null;
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
    public static class KodeverdiConverter implements AttributeConverter<KodeEndringLinje, String> {
        @Override
        public String convertToDatabaseColumn(KodeEndringLinje attribute) {
            return attribute.getKode();
        }

        @Override
        public KodeEndringLinje convertToEntityAttribute(String dbData) {
            return fraKode(dbData);
        }
    }
}
