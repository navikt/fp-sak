package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
public enum VilkårResultatType implements Kodeverdi {
     INNVILGET("INNVILGET", "Innvilget"),
     AVSLÅTT("AVSLAATT", "Avslått"),
     IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
    
     UDEFINERT("-", "Ikke definert"),

    ;
    
    private static final Map<String, VilkårResultatType> KODER = new LinkedHashMap<>();
    
    public static final String KODEVERK = "VILKAR_RESULTAT_TYPE";

    @JsonIgnore
    private String navn;

    private String kode;

    private VilkårResultatType(String kode) {
        this.kode = kode;
    }

    private VilkårResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static VilkårResultatType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårResultatType: " + kode);
        }
        return ad;
    }
    
    public static Map<String, VilkårResultatType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(a -> "\"" + a + "\"").collect(Collectors.toList()));
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
    
    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }
    
    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VilkårResultatType, String> {
        @Override
        public String convertToDatabaseColumn(VilkårResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
