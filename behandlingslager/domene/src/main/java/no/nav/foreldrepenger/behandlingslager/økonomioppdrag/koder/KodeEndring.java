package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KodeEndring implements Kodeverdi {
    NY("NY"),
    ENDRING("ENDR"),
    UENDRET("UEND")
    ;

    private static final String KODEVERK = "KODE_ENDRING_TYPE";
    private static final Map<String, KodeEndring> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;

    KodeEndring(String kode) {
        this.kode = kode;
    }

    public static KodeEndring fraKode(String kode) {
        Objects.requireNonNull(kode, "kodeEndring");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KodeEndring: " + kode);
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
    public static class KodeverdiConverter implements AttributeConverter<KodeEndring, String> {
        @Override
        public String convertToDatabaseColumn(KodeEndring attribute) {
            return attribute.getKode();
        }

        @Override
        public KodeEndring convertToEntityAttribute(String dbData) {
            return fraKode(dbData);
        }
    }
}
