package no.nav.foreldrepenger.behandlingslager.behandling.innsyn;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum InnsynResultatType implements Kodeverdi {

    INNVILGET("INNV", "Innvilget innsyn"),
    DELVIS_INNVILGET("DELV", "Delvis innvilget innsyn"),
    AVVIST("AVVIST", "Avslått innsyn"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, InnsynResultatType> KODER = new LinkedHashMap<>();

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

    InnsynResultatType(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<InnsynResultatType, String> {
        @Override
        public String convertToDatabaseColumn(InnsynResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public InnsynResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static InnsynResultatType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent InnsynResultatType: " + kode);
            }
            return ad;
        }
    }
}
