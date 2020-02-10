package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BeregningsgrunnlagRegelType implements Kodeverdi {

    SKJÆRINGSTIDSPUNKT("SKJÆRINGSTIDSPUNKT", "Fastsette skjæringstidspunkt"),
    BRUKERS_STATUS("BRUKERS_STATUS", "Fastsette brukers status/aktivitetstatus"),
    PERIODISERING("PERIODISERING", "Periodiser beregningsgrunnlag"),
    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "BG_REGEL_TYPE";
    @Deprecated
    public static final String DISCRIMINATOR = "BG_REGEL_TYPE";
    private static final Map<String, BeregningsgrunnlagRegelType> KODER = new LinkedHashMap<>();

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

    BeregningsgrunnlagRegelType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static BeregningsgrunnlagRegelType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningsgrunnlagRegelType: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningsgrunnlagRegelType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<BeregningsgrunnlagRegelType, String> {

        @Override
        public String convertToDatabaseColumn(BeregningsgrunnlagRegelType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BeregningsgrunnlagRegelType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
