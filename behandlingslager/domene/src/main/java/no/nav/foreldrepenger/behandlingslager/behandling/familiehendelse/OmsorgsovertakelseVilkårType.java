package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

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
public enum OmsorgsovertakelseVilkårType implements Kodeverdi {

    OMSORGSVILKÅRET("FP_VK_5", "Omsorgsvilkår §14-17 tredje ledd"),
    FORELDREANSVARSVILKÅRET_2_LEDD("FP_VK_8", "Foreldreansvarsvilkåret §14-17 andre ledd"),
    FORELDREANSVARSVILKÅRET_4_LEDD("FP_VK_33", "Foreldreansvarsvilkåret §14-17 fjerde ledd"),
    
    /* Legger inn udefinert kode. Må gjerne erstattes av noe annet dersom starttilstand er kjent. */
    UDEFINERT("-", "Ikke definert"),

    ;
    
    private static final Map<String, OmsorgsovertakelseVilkårType> KODER = new LinkedHashMap<>();
    
    public static final String KODEVERK = "OMSORGSOVERTAKELSE_VILKAR";

    @JsonIgnore
    private String navn;
    
    private String kode;

    private OmsorgsovertakelseVilkårType(String kode) {
        this.kode = kode;
    }

    private OmsorgsovertakelseVilkårType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static OmsorgsovertakelseVilkårType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OmsorgsovertakelseVilkårType: " + kode);
        }
        return ad;
    }
    
    public static Map<String, OmsorgsovertakelseVilkårType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OmsorgsovertakelseVilkårType, String> {
        @Override
        public String convertToDatabaseColumn(OmsorgsovertakelseVilkårType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OmsorgsovertakelseVilkårType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
