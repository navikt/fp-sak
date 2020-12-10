package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum KontrollerAktivitetskravAvklaring implements Kodeverdi {

    I_AKTIVITET("I_AKTIVITET", "Mor er i aktivitet"),
    IKKE_I_AKTIVITET_IKKE_DOKUMENTERT("IKKE_I_AKTIVITET_IKKE_DOKUMENTERT", "Aktiviteten er ikke dokumentert"),
    IKKE_I_AKTIVITET_DOKUMENTERT("IKKE_I_AKTIVITET_DOKUMENTERT", "Mor er ikke i aktivitet");

    public static final String KODEVERK = "AKTIVITETSKRAV_AVKLARING";

    private static final Map<String, KontrollerAktivitetskravAvklaring> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    private String kode;

    KontrollerAktivitetskravAvklaring(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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

    public static Map<String, KontrollerAktivitetskravAvklaring> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KontrollerAktivitetskravAvklaring fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(KontrollerAktivitetskravAvklaring.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MorsAktivitet: " + kode);
        }
        return ad;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KontrollerAktivitetskravAvklaring, String> {
        @Override
        public String convertToDatabaseColumn(KontrollerAktivitetskravAvklaring attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KontrollerAktivitetskravAvklaring convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
