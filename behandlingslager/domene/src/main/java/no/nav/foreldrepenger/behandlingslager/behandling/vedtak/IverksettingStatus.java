package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

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
public enum IverksettingStatus implements Kodeverdi {

    IKKE_IVERKSATT("IKKE_IVERKSATT", "Ikke iverksatt"),
    UNDER_IVERKSETTING("UNDER_IVERKSETTING", "Under iverksetting"),
    IVERKSATT("IVERKSATT", "Iverksatt"),

    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "IVERKSETTING_STATUS"; //$NON-NLS-1$
    private static final Map<String, IverksettingStatus> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;
    
    private String kode;

    private IverksettingStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static IverksettingStatus fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent IverksettingStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, IverksettingStatus> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<IverksettingStatus, String> {
        @Override
        public String convertToDatabaseColumn(IverksettingStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public IverksettingStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
