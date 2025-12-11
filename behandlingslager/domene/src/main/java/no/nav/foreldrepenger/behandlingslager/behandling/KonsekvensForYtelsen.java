package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KonsekvensForYtelsen implements Kodeverdi{

    FORELDREPENGER_OPPHØRER("FORELDREPENGER_OPPHØRER", "Foreldrepenger opphører"),
    ENDRING_I_BEREGNING("ENDRING_I_BEREGNING", "Endring i beregning"),
    ENDRING_I_UTTAK("ENDRING_I_UTTAK", "Endring i uttak"),
    ENDRING_I_FORDELING_AV_YTELSEN("ENDRING_I_FORDELING_AV_YTELSEN", "Endring i fordeling av ytelsen"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    ;

    private static final Map<String, KonsekvensForYtelsen> KODER = new LinkedHashMap<>();

    private String navn;

    @JsonValue
    private String kode;

    KonsekvensForYtelsen(String kode, String navn) {
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

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KonsekvensForYtelsen, String> {
        @Override
        public String convertToDatabaseColumn(KonsekvensForYtelsen attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KonsekvensForYtelsen convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static KonsekvensForYtelsen fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent KonsekvensForYtelsen: " + kode);
            }
            return ad;
        }

    }

}
