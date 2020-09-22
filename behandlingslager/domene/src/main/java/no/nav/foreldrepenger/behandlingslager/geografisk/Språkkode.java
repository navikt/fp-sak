package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
public enum Språkkode implements Kodeverdi {

    /**
     * Konstanter for å skrive ned kodeverdi.
     */
    NB("NB", "Norsk", "NB"),
    NN("NN", "Nynorsk", "NN"),
    EN("EN", "Engelsk", "EN"),

    UDEFINERT("-", "Ikke definert", null),
    ;

    public static final String KODEVERK = "SPRAAK_KODE";
    private static final Map<String, Språkkode> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;

    Språkkode(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Språkkode fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(Språkkode.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Språkkode: " + kode);
        }
        return ad;
    }

    public static Map<String, Språkkode> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Språkkode, String> {
        @Override
        public String convertToDatabaseColumn(Språkkode attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Språkkode convertToEntityAttribute(String dbData) {
            return dbData == null ? null : defaultNorsk(dbData);
        }
    }


    public static Språkkode finnForKodeverkEiersKode(String offisiellSpråkkode) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellSpråkkode)).findFirst().orElse(Språkkode.NB);
    }

    public static Språkkode defaultNorsk(String kode) {
        return kode == null ? Språkkode.NB : KODER.getOrDefault(kode, Språkkode.NB);
    }
}
