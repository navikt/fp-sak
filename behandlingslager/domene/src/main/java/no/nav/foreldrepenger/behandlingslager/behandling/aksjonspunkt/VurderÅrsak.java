package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

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

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum VurderÅrsak implements Kodeverdi {

    FEIL_FAKTA("FEIL_FAKTA", "Feil fakta"),
    FEIL_LOV("FEIL_LOV", "Feil lovanvendelse"),
    FEIL_REGEL("FEIL_REGEL", "Feil regelforståelse"),
    ANNET("ANNET", "Annet"),
    UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VurderÅrsak> KODER = new LinkedHashMap<>();
    public static final String KODEVERK = "VURDER_AARSAK";

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

    private VurderÅrsak() {
    }

    private VurderÅrsak(String kode) {
        this.kode = kode;
    }

    private VurderÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static VurderÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VurderÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, VurderÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Override
    public String getNavn() {
        return navn;
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

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VurderÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(VurderÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VurderÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
