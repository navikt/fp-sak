package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum StønadskontoType implements Kodeverdi {

    FELLESPERIODE("FELLESPERIODE", "Fellesperiode"),
    MØDREKVOTE("MØDREKVOTE", "Mødrekvote"),
    FEDREKVOTE("FEDREKVOTE", "Fedrekvote"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    FLERBARNSDAGER("FLERBARNSDAGER", "Flerbarnsdager"),
    FORELDREPENGER_FØR_FØDSEL("FORELDREPENGER_FØR_FØDSEL", "Foreldrepenger før fødsel"),
    UDEFINERT("-", "Ikke valgt stønadskonto"),
    ;

    private static final Map<String, StønadskontoType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "STOENADSKONTOTYPE";

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

    StønadskontoType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, StønadskontoType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<StønadskontoType, String> {
        @Override
        public String convertToDatabaseColumn(StønadskontoType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public StønadskontoType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static StønadskontoType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent StønadskontoType: " + kode);
            }
            return ad;
        }
    }
}
