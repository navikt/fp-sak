package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VilkårUtfallType implements Kodeverdi {
    OPPFYLT("OPPFYLT", "Oppfylt"),
    IKKE_OPPFYLT("IKKE_OPPFYLT", "Ikke oppfylt"),
    IKKE_VURDERT("IKKE_VURDERT", "Ikke vurdert"),

    UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VilkårUtfallType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VILKAR_UTFALL_TYPE";


    private String navn;
    @JsonValue
    private String kode;

    VilkårUtfallType(String kode) {
        this.kode = kode;
    }

    VilkårUtfallType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, VilkårUtfallType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static boolean erFastsatt(VilkårUtfallType type) {
        return OPPFYLT.equals(type) || IKKE_OPPFYLT.equals(type);
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VilkårUtfallType, String> {
        @Override
        public String convertToDatabaseColumn(VilkårUtfallType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårUtfallType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static VilkårUtfallType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent VilkårUtfallType: " + kode);
            }
            return ad;
        }
    }

}
