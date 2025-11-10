package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum BeregningSatsType implements Kodeverdi {
    ENGANG("ENGANG", "Engangsstønad"),
    GRUNNBELØP("GRUNNBELØP", "Grunnbeløp"),
    GSNITT("GSNITT", "Grunnbeløp årsgjennomsnitt"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;
    public static final String KODEVERK = "SATS_TYPE";

    private static final Map<String, BeregningSatsType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    BeregningSatsType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, BeregningSatsType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<BeregningSatsType, String> {

        public static BeregningSatsType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent BeregningSatsType: " + kode);
            }
            return ad;
        }

        @Override
        public String convertToDatabaseColumn(BeregningSatsType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BeregningSatsType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
