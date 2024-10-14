package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VilkårResultatType implements Kodeverdi {
     INNVILGET("INNVILGET", "Innvilget"),
     AVSLÅTT("AVSLAATT", "Avslått"),
     IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),

     UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VilkårResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VILKAR_RESULTAT_TYPE";

    private final String navn;

    @JsonValue
    private final String kode;

    VilkårResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, VilkårResultatType> kodeMap() {
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VilkårResultatType, String> {
        @Override
        public String convertToDatabaseColumn(VilkårResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static VilkårResultatType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent VilkårResultatType: " + kode);
            }
            return ad;
        }
    }

}
