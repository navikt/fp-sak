package no.nav.foreldrepenger.behandlingslager.behandling.verge;

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
public enum VergeType implements Kodeverdi {

    BARN("BARN", "Verge for barn under 18 år"),
    FBARN("FBARN", "Verge for foreldreløst barn under 18 år"),
    VOKSEN("VOKSEN", "Verge for voksen"),
    ADVOKAT("ADVOKAT", "Advokat/advokatfullmektig"),
    ANNEN_F("ANNEN_F", "Annen fullmektig"),
    UDEFINERT("-", "Ikke satt eller valgt kode"),;

    private static final Map<String, VergeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VERGE_TYPE";

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

    VergeType(String kode) {
        this.kode = kode;
    }

    VergeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VergeType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        var kode = TempAvledeKode.getVerdi(VergeType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VergeType: " + kode);
        }
        return ad;
    }

    public static Map<String, VergeType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<VergeType, String> {
        @Override
        public String convertToDatabaseColumn(VergeType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VergeType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

    }

}
