package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

// TODO: Kanskje man bør ha et mer spesifikt navn
public enum VilkårHjemmel implements Kodeverdi {
    F_14_5_1_FP("F_14_5_1", "Adopsjonsvilkåret § 14-5, 1.ledd"),
    F_14_5_3_FP("F_14_5_3", "Adopsjonsvilkåret § 14-5, 3. ledd"),
    F_14_5_2_FP("F_14_5_2", "Foreldreansvarsvilkåret § 14-5, 2. ledd"),

    F_14_17_1_ES("F_14_17_1", "Adopsjonsvilkåret § 14-17, 1. ledd"),
    F_14_17_3_ES("F_14_17_3", "Omsorgsvilkåret § 14-17, 3. ledd"),
    F_14_17_2_ES("F_14_17_2", "Foreldreansvarsvilkåret § 14-17, 2. ledd"),
    F_14_17_4_ES("F_14_17_4", "Foreldreansvarsvilkåret § 14-17, 4. ledd"),

    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "VILKAAR_HJEMMEL"; //kan denne ha ÆØÅ?
    private static final Map<String, VilkårHjemmel> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;
    @JsonValue
    private final String kode;

    VilkårHjemmel(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, VilkårHjemmel> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<VilkårHjemmel, String> {
        private static VilkårHjemmel fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent VilkårHjemmel: " + kode);
            }
            return ad;
        }

        @Override
        public String convertToDatabaseColumn(VilkårHjemmel attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårHjemmel convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
