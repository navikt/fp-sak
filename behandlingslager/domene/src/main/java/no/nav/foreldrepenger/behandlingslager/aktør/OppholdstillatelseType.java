package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum OppholdstillatelseType implements Kodeverdi {

    MIDLERTIDIG("MIDLERTIDIG", "Midlertidig oppholdstillatelse"),
    PERMANENT("PERMANENT", "Permanent oppholdstillatelse"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, OppholdstillatelseType> KODER = Arrays.stream(values())
        .collect(Collectors.toMap(OppholdstillatelseType::getKode, Function.identity()));

    private String navn;

    @JsonValue
    private String kode;

    OppholdstillatelseType(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<OppholdstillatelseType, String> {
        @Override
        public String convertToDatabaseColumn(OppholdstillatelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OppholdstillatelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static OppholdstillatelseType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent OppholdstillatelseType: " + kode);
            }
            return ad;
        }
    }

}
