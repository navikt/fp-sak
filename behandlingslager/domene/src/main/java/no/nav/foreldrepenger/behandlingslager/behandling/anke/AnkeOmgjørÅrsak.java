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
public enum AnkeOmgjørÅrsak implements Kodeverdi {

    NYE_OPPLYSNINGER("NYE_OPPLYSNINGER", "Nye opplysninger"),
    ULIK_REGELVERKSTOLKNING("ULIK_REGELVERKSTOLKNING", "Ulik regelverkstolkning"),
    ULIK_VURDERING("ULIK_VURDERING", "Ulik skjønnsvurdering"),
    PROSESSUELL_FEIL("PROSESSUELL_FEIL", "Saksbehandlingsfeil"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, AnkeOmgjørÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "ANKE_OMGJOER_AARSAK";

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

    private AnkeOmgjørÅrsak(String kode) {
        this.kode = kode;
    }

    private AnkeOmgjørÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AnkeOmgjørÅrsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(AnkeOmgjørÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AnkeOmgjørÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, AnkeOmgjørÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<AnkeOmgjørÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(AnkeOmgjørÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AnkeOmgjørÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
