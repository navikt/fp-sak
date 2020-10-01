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
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;


@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum AnkeVurderingOmgjør implements Kodeverdi {

    ANKE_TIL_GUNST("ANKE_TIL_GUNST", "Gunst omgjør i anke"),
    ANKE_DELVIS_OMGJOERING_TIL_GUNST("ANKE_DELVIS_OMGJOERING_TIL_GUNST", "Delvis omgjøring, til gunst i anke"),
    ANKE_TIL_UGUNST("ANKE_TIL_UGUNST", "Ugunst omgjør i anke"),
    UDEFINERT("-", "Udefinert"),
    ;
    private static final Map<String, AnkeVurderingOmgjør> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ANKE_VURDERING_OMGJOER";

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

    private AnkeVurderingOmgjør(String kode) {
        this.kode = kode;
    }

    private AnkeVurderingOmgjør(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AnkeVurderingOmgjør fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(AnkeVurderingOmgjør.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AnkeVurderingOmgjør: " + kode);
        }
        return ad;
    }
    public static Map<String, AnkeVurderingOmgjør> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<AnkeVurderingOmgjør, String> {
        @Override
        public String convertToDatabaseColumn(AnkeVurderingOmgjør attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AnkeVurderingOmgjør convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
}
    }
