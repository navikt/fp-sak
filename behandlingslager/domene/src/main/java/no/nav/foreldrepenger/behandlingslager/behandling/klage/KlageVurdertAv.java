package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KlageVurdertAv implements Kodeverdi {

    NFP("NFP", "Nav familie- og pensjonsytelser"),
    NK("NK", "Nav klageinstans"),
    ;

    private static final Map<String, KlageVurdertAv> KODER = new LinkedHashMap<>();

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

    @Override
    public String getNavn() {
        return navn;
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
