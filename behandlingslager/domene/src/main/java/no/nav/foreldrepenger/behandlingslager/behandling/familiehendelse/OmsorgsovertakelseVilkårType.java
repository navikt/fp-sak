package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum OmsorgsovertakelseVilkårType implements Kodeverdi {

    OMSORGSVILKÅRET("FP_VK_5", "Omsorgsvilkår §14-17 tredje ledd"),
    FORELDREANSVARSVILKÅRET_2_LEDD("FP_VK_8", "Foreldreansvarsvilkåret §14-17 andre ledd"),
    FORELDREANSVARSVILKÅRET_4_LEDD("FP_VK_33", "Foreldreansvarsvilkåret §14-17 fjerde ledd"),

    /* Legger inn udefinert kode. Må gjerne erstattes av noe annet dersom starttilstand er kjent. */
    UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, OmsorgsovertakelseVilkårType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OMSORGSOVERTAKELSE_VILKAR";

    private String navn;

    @JsonValue
    private String kode;

    OmsorgsovertakelseVilkårType(String kode) {
        this.kode = kode;
    }

    OmsorgsovertakelseVilkårType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, OmsorgsovertakelseVilkårType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<OmsorgsovertakelseVilkårType, String> {
        @Override
        public String convertToDatabaseColumn(OmsorgsovertakelseVilkårType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OmsorgsovertakelseVilkårType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static OmsorgsovertakelseVilkårType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent OmsorgsovertakelseVilkårType: " + kode);
            }
            return ad;
        }
    }
}
