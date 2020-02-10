package no.nav.foreldrepenger.domene.risikoklassifisering.modell;

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
public enum FaresignalVurdering implements Kodeverdi {

    INNVIRKNING("INNVIRKNING", "Faresignalene hadde innvirkning på behandlingen"),
    INGEN_INNVIRKNING("INGEN_INNVIRKNING", "Faresignalene hadde ingen innvirkning på behandlingen"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, FaresignalVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FARESIGNAL_VURDERING";

    @Deprecated
    public static final String DISCRIMINATOR = "FARESIGNAL_VURDERING";

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

    private FaresignalVurdering(String kode) {
        this.kode = kode;
    }

    private FaresignalVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static FaresignalVurdering fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FaresignalVurdering: " + kode);
        }
        return ad;
    }

    public static Map<String, FaresignalVurdering> kodeMap() {
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<FaresignalVurdering, String> {
        @Override
        public String convertToDatabaseColumn(FaresignalVurdering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FaresignalVurdering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
