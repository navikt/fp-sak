package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
public enum KlageVurdering implements Kodeverdi {

    OPPHEVE_YTELSESVEDTAK("OPPHEVE_YTELSESVEDTAK", "Ytelsesvedtaket oppheves"),
    STADFESTE_YTELSESVEDTAK("STADFESTE_YTELSESVEDTAK", "Ytelsesvedtaket stadfestes"),
    MEDHOLD_I_KLAGE("MEDHOLD_I_KLAGE", "Medhold"),
    AVVIS_KLAGE("AVVIS_KLAGE", "Klagen avvises"),
    HJEMSENDE_UTEN_Å_OPPHEVE("HJEMSENDE_UTEN_Å_OPPHEVE", "Hjemsende uten å oppheve"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, KlageVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGEVURDERING";

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

    private KlageVurdering(String kode) {
        this.kode = kode;
    }

    private KlageVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static KlageVurdering fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageVurdering: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageVurdering> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<KlageVurdering, String> {
        @Override
        public String convertToDatabaseColumn(KlageVurdering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageVurdering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    /** Kun til invortes bruk i tester. Ingen garanti for at dette dekker alle konstanter. */
    public static Map<String, KlageVurdering> getHardkodedeKonstanter() {
        return Set.of(values()).stream().collect(Collectors.toMap(v -> v.getKode(), v -> v));
    }
}
