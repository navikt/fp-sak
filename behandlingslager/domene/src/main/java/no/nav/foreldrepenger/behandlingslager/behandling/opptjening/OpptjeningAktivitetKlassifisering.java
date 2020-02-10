package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

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
public enum OpptjeningAktivitetKlassifisering implements Kodeverdi {

    BEKREFTET_GODKJENT("BEKREFTET_GODKJENT", "Bekreftet godkjent"),
    BEKREFTET_AVVIST("BEKREFTET_AVVIST", "Bekreftet avvist"),
    ANTATT_GODKJENT("ANTATT_GODKJENT", "Antatt godkjent"),
    MELLOMLIGGENDE_PERIODE("MELLOMLIGGENDE_PERIODE", "Mellomliggende periode"),
    UDEFINERT("-", "UDEFINERT"),
    ;

    private static final Map<String, OpptjeningAktivitetKlassifisering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPTJENING_AKTIVITET_KLASSIFISERING";

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

    private OpptjeningAktivitetKlassifisering(String kode) {
        this.kode = kode;
    }

    private OpptjeningAktivitetKlassifisering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static OpptjeningAktivitetKlassifisering fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OpptjeningAktivitetKlassifisering: " + kode);
        }
        return ad;
    }

    public static Map<String, OpptjeningAktivitetKlassifisering> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OpptjeningAktivitetKlassifisering, String> {
        @Override
        public String convertToDatabaseColumn(OpptjeningAktivitetKlassifisering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OpptjeningAktivitetKlassifisering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}