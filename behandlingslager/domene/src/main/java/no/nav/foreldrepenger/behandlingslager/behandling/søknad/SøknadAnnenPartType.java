
package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SøknadAnnenPartType implements Kodeverdi {

    MOR("MOR", "Mor"),
    MEDMOR("MEDMOR", "Medmor"),
    FAR("FAR", "Far"),
    MEDFAR("MEDFAR", "Medfar"),
    UDEFINERT("-", "Ikke definert"),
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

    @JsonIgnore
    private String navn;

    private String kode;

    private SøknadAnnenPartType(String kode) {
        this.kode = kode;
    }

    private SøknadAnnenPartType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static SøknadAnnenPartType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SøknadAnnenPartType: " + kode);
        }
        return ad;
    }

    public static Map<String, SøknadAnnenPartType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
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
    }
}
