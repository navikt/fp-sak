package no.nav.foreldrepenger.domene.modell;

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
public enum BeregningsgrunnlagPeriodeRegelType implements Kodeverdi {
    FORESLÅ("FORESLÅ", "Foreslå beregningsgrunnlag"),
    VILKÅR_VURDERING("VILKÅR_VURDERING", "Vurder beregningsvilkår"),
    FORDEL("FORDEL", "Fordel beregningsgrunnlag"),
    FASTSETT("FASTSETT", "Fastsett/fullføre beregningsgrunnlag"),
    OPPDATER_GRUNNLAG_SVP("OPPDATER_GRUNNLAG_SVP", "Oppdater grunnlag for SVP"),
    FASTSETT2("FASTSETT2", "Fastsette/fullføre beregningsgrunnlag for andre gangs kjøring for SVP"),
    FINN_GRENSEVERDI("FINN_GRENSEVERDI", "Finne grenseverdi til kjøring av fastsett beregningsgrunnlag for SVP"),
    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "BG_PERIODE_REGEL_TYPE";

    private static final Map<String, BeregningsgrunnlagPeriodeRegelType> KODER = new LinkedHashMap<>();

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

    BeregningsgrunnlagPeriodeRegelType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static BeregningsgrunnlagPeriodeRegelType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningsgrunnlagPeriodeRegelType: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningsgrunnlagPeriodeRegelType> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BeregningsgrunnlagPeriodeRegelType, String> {

        @Override
        public String convertToDatabaseColumn(BeregningsgrunnlagPeriodeRegelType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BeregningsgrunnlagPeriodeRegelType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
