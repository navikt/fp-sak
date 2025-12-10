package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum NavBrukerKjønn implements Kodeverdi {

    KVINNE("K", "Kvinne"),
    MANN("M", "Mann"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, NavBrukerKjønn> KODER = new LinkedHashMap<>();

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

    NavBrukerKjønn(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<NavBrukerKjønn, String> {
        @Override
        public String convertToDatabaseColumn(NavBrukerKjønn attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public NavBrukerKjønn convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static NavBrukerKjønn fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent NavBrukerKjønn: " + kode);
            }
            return ad;
        }
    }

}
