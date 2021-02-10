package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KodeStatusLinje implements Kodeverdi {
    OPPHØR("OPPH")
    ;

    private static final String KODEVERK = "KODE_STATUS_LINJE_TYPE";
    private static final Map<String, KodeStatusLinje> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;

    KodeStatusLinje(String kode) {
        this.kode = kode;
    }

    public static KodeStatusLinje fraKode(String kode) {
        Objects.requireNonNull(kode, "kodeStatusLinje");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KodeStatusLinje: " + kode);
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
    public static class KodeverdiConverter implements AttributeConverter<KodeStatusLinje, String> {
        @Override
        public String convertToDatabaseColumn(KodeStatusLinje attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KodeStatusLinje convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
