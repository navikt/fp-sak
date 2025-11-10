
package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum SøknadAnnenPartType implements Kodeverdi {

    MOR("MOR", "Mor"),
    MEDMOR("MEDMOR", "Medmor"),
    FAR("FAR", "Far"),
    MEDFAR("MEDFAR", "Medfar"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, SøknadAnnenPartType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SOEKNAD_ANNEN_PART";

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

    SøknadAnnenPartType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, SøknadAnnenPartType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<SøknadAnnenPartType, String> {
        @Override
        public String convertToDatabaseColumn(SøknadAnnenPartType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public SøknadAnnenPartType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static SøknadAnnenPartType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent SøknadAnnenPartType: " + kode);
            }
            return ad;
        }
    }
}
