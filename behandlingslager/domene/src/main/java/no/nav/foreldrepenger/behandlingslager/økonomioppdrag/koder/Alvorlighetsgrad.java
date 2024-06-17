package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum Alvorlighetsgrad {
    OK("00"),
    OK_MED_MERKNAD("04"),
    FEIL("08");

    private static final Map<String, Alvorlighetsgrad> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;

    Alvorlighetsgrad(String kode) {
        this.kode = kode;
    }

    public static Alvorlighetsgrad fraKode(String kode) {
        Objects.requireNonNull(kode, "alvorlighetsgrad");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Alvorlighetsgrad: " + kode);
        }
        return ad;
    }

    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Alvorlighetsgrad, String> {
        @Override
        public String convertToDatabaseColumn(Alvorlighetsgrad attribute) {
            return attribute.getKode();
        }

        @Override
        public Alvorlighetsgrad convertToEntityAttribute(String dbData) {
            return fraKode(dbData);
        }
    }
}
