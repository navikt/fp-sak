package no.nav.foreldrepenger.domene.modell;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Hjemmel implements Kodeverdi {

    F_14_7("F_14_7", "folketrygdloven § 14-7"),
    F_14_7_8_30("F_14_7_8_30", "folketrygdloven §§ 14-7 og 8-30"),
    F_14_7_8_28_8_30("F_14_7_8_28_8_30", "folketrygdloven §§ 14-7, 8-28 og 8-30"),
    F_14_7_8_35("F_14_7_8_35", "folketrygdloven §§ 14-7 og 8-35"),
    F_14_7_8_38("F_14_7_8_38", "folketrygdloven §§ 14-7 og 8-38"),
    F_14_7_8_40("F_14_7_8_40", "folketrygdloven §§ 14-7 og 8-40"),
    F_14_7_8_41("F_14_7_8_41", "folketrygdloven §§ 14-7 og 8-41"),
    F_14_7_8_42("F_14_7_8_42", "folketrygdloven §§ 14-7 og 8-42"),
    F_14_7_8_43("F_14_7_8_43", "folketrygdloven §§ 14-7 og 8-43"),
    F_14_7_8_47("F_14_7_8_47", "folketrygdloven §§ 14-7 og 8-47"),
    F_14_7_8_49("F_14_7_8_49", "folketrygdloven §§ 14-7 og 8-49"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, Hjemmel> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BG_HJEMMEL";

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

    Hjemmel(String kode) {
        this.kode = kode;
    }

    Hjemmel(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Hjemmel fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Hjemmel: " + kode);
        }
        return ad;
    }

    public static Map<String, Hjemmel> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<Hjemmel, String> {
        @Override
        public String convertToDatabaseColumn(Hjemmel attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Hjemmel convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
