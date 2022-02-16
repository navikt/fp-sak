package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
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
public enum OppholdstillatelseType implements Kodeverdi {

    MIDLERTIDIG("MIDLERTIDIG", "Midlertidig oppholdstillatelse"),
    PERMANENT("PERMANENT", "Permanent oppholdstillatelse"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, OppholdstillatelseType> KODER = Arrays.stream(values())
        .collect(Collectors.toMap(OppholdstillatelseType::getKode, Function.identity()));

    public static final String KODEVERK = "OPPHOLDSTILLATELSE_TYPE";

    @JsonIgnore
    private String navn;

    private String kode;

    OppholdstillatelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static OppholdstillatelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OppholdstillatelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, OppholdstillatelseType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OppholdstillatelseType, String> {
        @Override
        public String convertToDatabaseColumn(OppholdstillatelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OppholdstillatelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
