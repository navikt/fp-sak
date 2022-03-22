package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KlageVurdertAv implements Kodeverdi {

    NFP("NFP", "NAV Familie- og Pensjonsytelser"),
    NK("NK", "NAV Klageinstans"),
    ;

    private static final Map<String, KlageVurdertAv> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_VURDERT_AV";

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

    KlageVurdertAv(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }


    public static Map<String, KlageVurdertAv> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<KlageVurdertAv, String> {
        @Override
        public String convertToDatabaseColumn(KlageVurdertAv attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageVurdertAv convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static KlageVurdertAv fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent KlageVurdertAv: " + kode);
            }
            return ad;
        }
    }
}
