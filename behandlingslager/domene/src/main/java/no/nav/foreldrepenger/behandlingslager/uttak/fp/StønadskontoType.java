package no.nav.foreldrepenger.behandlingslager.uttak.fp;

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
public enum StønadskontoType implements Kodeverdi {

    FELLESPERIODE("FELLESPERIODE", "Fellesperiode"),
    MØDREKVOTE("MØDREKVOTE", "Mødrekvote"),
    FEDREKVOTE("FEDREKVOTE", "Fedrekvote"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    FLERBARNSDAGER("FLERBARNSDAGER", "Flerbarnsdager"),
    FORELDREPENGER_FØR_FØDSEL("FORELDREPENGER_FØR_FØDSEL", "Foreldrepenger før fødsel"),
    UDEFINERT("-", "Ikke valgt stønadskonto"),
    ;

    private static final Map<String, StønadskontoType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "STOENADSKONTOTYPE";

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

    private StønadskontoType(String kode) {
        this.kode = kode;
    }

    private StønadskontoType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StønadskontoType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(StønadskontoType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent StønadskontoType: " + kode);
        }
        return ad;
    }

    public static Map<String, StønadskontoType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<StønadskontoType, String> {
        @Override
        public String convertToDatabaseColumn(StønadskontoType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public StønadskontoType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
