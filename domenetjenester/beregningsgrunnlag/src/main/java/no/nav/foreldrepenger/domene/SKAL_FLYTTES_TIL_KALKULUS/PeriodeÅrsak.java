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
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum PeriodeÅrsak implements Kodeverdi {

    NATURALYTELSE_BORTFALT("NATURALYTELSE_BORTFALT", "Naturalytelse bortfalt"),
    ARBEIDSFORHOLD_AVSLUTTET("ARBEIDSFORHOLD_AVSLUTTET", "Arbeidsforhold avsluttet"),
    NATURALYTELSE_TILKOMMER("NATURALYTELSE_TILKOMMER", "Naturalytelse tilkommer"),
    ENDRING_I_REFUSJONSKRAV("ENDRING_I_REFUSJONSKRAV", "Endring i refusjonskrav"),
    REFUSJON_OPPHØRER("REFUSJON_OPPHØRER", "Refusjon opphører"),
    GRADERING("GRADERING", "Gradering"),
    GRADERING_OPPHØRER("GRADERING_OPPHØRER", "Gradering opphører"),
    ENDRING_I_AKTIVITETER_SØKT_FOR("ENDRING_I_AKTIVITETER_SØKT_FOR", "Endring i aktiviteter søkt for"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, PeriodeÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERIODE_AARSAK";

    @Deprecated
    public static final String DISCRIMINATOR = "PERIODE_AARSAK";

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

    private PeriodeÅrsak(String kode) {
        this.kode = kode;
    }

    private PeriodeÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static PeriodeÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PeriodeÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, PeriodeÅrsak> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<PeriodeÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(PeriodeÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public PeriodeÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
