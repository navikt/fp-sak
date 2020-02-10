package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/** Fra offisielt kodeverk (kodeverkklienten). */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SivilstandType implements Kodeverdi {

    ENKEMANN("ENKE", "Enke/-mann"),
    GIFT("GIFT", "Gift"),
    GJENLEVENDE_PARTNER("GJPA", "Gjenlevende partner"),
    GIFT_ADSKILT("GLAD", "Gift, lever adskilt"),
    UOPPGITT("NULL", "Uoppgitt"),
    REGISTRERT_PARTNER("REPA", "Registrert partner"),
    SAMBOER("SAMB", "Samboer"),
    SEPARERT_PARTNER("SEPA", "Separert partner"),
    SEPARERT("SEPR", "Separert"),
    SKILT("SKIL", "Skilt"),
    SKILT_PARTNER("SKPA", "Skilt partner"),
    UGIFT("UGIF", "Ugift"),
    ;

    public static final String KODEVERK = "SIVILSTAND_TYPE";

    private static final Map<String, SivilstandType> KODER = new LinkedHashMap<>();

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

    private SivilstandType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }
    
    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }
    
    @Override
    public String getNavn() {
        return navn;
    }
    
    @Override
    public String getOffisiellKode() {
        return getKode();
    }
    
    @JsonCreator
    public static SivilstandType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakYtelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, SivilstandType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    
    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<SivilstandType, String> {
        @Override
        public String convertToDatabaseColumn(SivilstandType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public SivilstandType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
