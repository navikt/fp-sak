package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum SivilstandType implements Kodeverdi {

    ENKEMANN("ENKE", "Enke/-mann"),
    GIFT("GIFT", "Gift"),
    GJENLEVENDE_PARTNER("GJPA", "Gjenlevende partner"),
    GIFT_ADSKILT("GLAD", "Gift, lever adskilt"),
    UOPPGITT("NULL", "Uoppgitt"),
    REGISTRERT_PARTNER("REPA", "Registrert partner"),
    SAMBOER("SAMB", "Samboer"),
    SEPARERT_PARTNER("SEPA", "Separert partner"),
    SEPARERT("SEPR", "Separert"),
    SKILT("SKIL", "Skilt"),
    SKILT_PARTNER("SKPA", "Skilt partner"),
    UGIFT("UGIF", "Ugift"),
    ;

    private static final Map<String, SivilstandType> KODER = new LinkedHashMap<>();

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

    SivilstandType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<SivilstandType, String> {
        @Override
        public String convertToDatabaseColumn(SivilstandType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public SivilstandType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static SivilstandType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent FagsakYtelseType: " + kode);
            }
            return ad;
        }
    }

}
