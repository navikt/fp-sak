package no.nav.foreldrepenger.behandlingslager.behandling;

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
public enum KonsekvensForYtelsen implements Kodeverdi{


    FORELDREPENGER_OPPHØRER("FORELDREPENGER_OPPHØRER", "Foreldrepenger opphører"),
    ENDRING_I_BEREGNING("ENDRING_I_BEREGNING", "Endring i beregning"),
    ENDRING_I_UTTAK("ENDRING_I_UTTAK", "Endring i uttak"),
    ENDRING_I_FORDELING_AV_YTELSEN("ENDRING_I_FORDELING_AV_YTELSEN", "Endring i fordeling av ytelsen"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    ENDRING_I_BEREGNING_OG_UTTAK("ENDRING_I_BEREGNING_OG_UTTAK", "Endring i beregning og uttak"),
    UDEFINERT("-", "Udefinert"),

    ;

    private static final Map<String, KonsekvensForYtelsen> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KONSEKVENS_FOR_YTELSEN";

    @JsonIgnore
    private String navn;

    private String kode;

    private KonsekvensForYtelsen(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }
    @JsonCreator
    public static KonsekvensForYtelsen fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KonsekvensForYtelsen: " + kode);
        }
        return ad;
    }

    public static Map<String, KonsekvensForYtelsen> kodeMap() {
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KonsekvensForYtelsen, String> {
        @Override
        public String convertToDatabaseColumn(KonsekvensForYtelsen attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KonsekvensForYtelsen convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }


}
