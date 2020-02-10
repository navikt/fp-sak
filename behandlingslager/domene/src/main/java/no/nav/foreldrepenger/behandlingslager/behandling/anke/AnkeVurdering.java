package no.nav.foreldrepenger.behandlingslager.behandling.anke;

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
public enum AnkeVurdering implements Kodeverdi {

    ANKE_STADFESTE_YTELSESVEDTAK("ANKE_STADFESTE_YTELSESVEDTAK", "Ytelsesvedtaket stadfestes"),
    ANKE_OPPHEVE_OG_HJEMSENDE("ANKE_OPPHEVE_OG_HJEMSENDE", "Ytelsesvedtaket oppheve og hjemsende"),
    ANKE_OMGJOER("ANKE_OMGJOER", "Anken omgjør"),
    ANKE_AVVIS("ANKE_AVVIS", "Anken avvises"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, AnkeVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ANKEVURDERING";

    @Deprecated
    public static final String DISCRIMINATOR = "ANKEVURDERING";

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

    private AnkeVurdering(String kode) {
        this.kode = kode;
    }

    private AnkeVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static AnkeVurdering fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AnkeVurdering: " + kode);
        }
        return ad;
    }

    public static Map<String, AnkeVurdering> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<AnkeVurdering, String> {
        @Override
        public String convertToDatabaseColumn(AnkeVurdering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AnkeVurdering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
