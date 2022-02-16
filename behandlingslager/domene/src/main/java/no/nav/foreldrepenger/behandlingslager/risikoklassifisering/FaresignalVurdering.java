package no.nav.foreldrepenger.behandlingslager.risikoklassifisering;

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
public enum FaresignalVurdering implements Kodeverdi {

    INNVIRKNING("INNVIRKNING", "Faresignalene vurderes som reelle"),
    INNVILGET_REDUSERT("INNVILGET_REDUSERT", "Saken er innvilget med redusert beregningsgrunnlag"),
    INNVILGET_UENDRET("INNVILGET_UENDRET", "Saken er innvilget uten at faresignalene påvirket utfallet"),
    AVSLAG_FARESIGNAL("AVSLAG_FARESIGNAL", "Saken er avslått på grunn av faresignalene"),
    AVSLAG_ANNET("AVSLAG_ANNET", "Saken er avslått av andre årsaker"),
    INGEN_INNVIRKNING("INGEN_INNVIRKNING", "Faresignalene vurderes ikke som reelle"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, FaresignalVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FARESIGNAL_VURDERING";

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

    FaresignalVurdering(String kode) {
        this.kode = kode;
    }

    FaresignalVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FaresignalVurdering fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        var kode = TempAvledeKode.getVerdi(FaresignalVurdering.class, node, "kode");
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
