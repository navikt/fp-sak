package no.nav.foreldrepenger.behandlingslager.behandling.klage;

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
public enum KlageMedholdÅrsak implements Kodeverdi {

    NYE_OPPLYSNINGER("NYE_OPPLYSNINGER", "Nytt faktum"),
    ULIK_REGELVERKSTOLKNING("ULIK_REGELVERKSTOLKNING", "Feil lovanvendelse"),
    ULIK_VURDERING("ULIK_VURDERING", "Ulik skjønnsvurdering"),
    PROSESSUELL_FEIL("PROSESSUELL_FEIL", "Saksbehandlingsfeil"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, KlageMedholdÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_MEDHOLD_AARSAK";

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

    private KlageMedholdÅrsak(String kode) {
        this.kode = kode;
    }

    private KlageMedholdÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static KlageMedholdÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageMedholdÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageMedholdÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<KlageMedholdÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(KlageMedholdÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageMedholdÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
