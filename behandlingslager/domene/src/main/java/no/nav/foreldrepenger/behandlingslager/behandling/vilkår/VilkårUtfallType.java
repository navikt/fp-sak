package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

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
public enum VilkårUtfallType implements Kodeverdi {
    OPPFYLT("OPPFYLT", "Oppfylt"),
    IKKE_OPPFYLT("IKKE_OPPFYLT", "Ikke oppfylt"),
    IKKE_VURDERT("IKKE_VURDERT", "Ikke vurdert"),
    IKKE_RELEVANT("IKKE_RELEVANT", "Ikke relevant"),
    AVSLAAS_I_ANNET_VILKAAR("AVSLAAS_I_ANNET_VILKAAR", "Avslås i annet vilkår"),

    UDEFINERT("-", "Ikke definert"),

    ;
    
    private static final Map<String, VilkårUtfallType> KODER = new LinkedHashMap<>();
    
    public static final String KODEVERK = "VILKAR_UTFALL_TYPE";

    @JsonIgnore
    private String navn;

    private String kode;

    private VilkårUtfallType(String kode) {
        this.kode = kode;
    }

    private VilkårUtfallType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static VilkårUtfallType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårUtfallType: " + kode);
        }
        return ad;
    }
    
    public static Map<String, VilkårUtfallType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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
    public static class KodeverdiConverter implements AttributeConverter<VilkårUtfallType, String> {
        @Override
        public String convertToDatabaseColumn(VilkårUtfallType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårUtfallType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
    
}
