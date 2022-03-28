package no.nav.foreldrepenger.domene.modell;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum BeregningAktivitetHandlingType implements Kodeverdi {

    BENYTT("BENYTT", "Benytt beregningaktivitet"),
    IKKE_BENYTT("IKKE_BENYTT", "Ikke benytt beregningaktivitet"),
    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "BEREGNING_AKTIVITET_HANDLING_TYPE";

    private static final Map<String, BeregningAktivitetHandlingType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    BeregningAktivitetHandlingType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BeregningAktivitetHandlingType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningAktivitetHandlingType: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningAktivitetHandlingType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BeregningAktivitetHandlingType, String> {

        @Override
        public String convertToDatabaseColumn(BeregningAktivitetHandlingType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BeregningAktivitetHandlingType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
