package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum AnkeOmgjørÅrsak implements Kodeverdi {

    NYE_OPPLYSNINGER("NYE_OPPLYSNINGER", "Nye opplysninger"),
    ULIK_REGELVERKSTOLKNING("ULIK_REGELVERKSTOLKNING", "Ulik regelverkstolkning"),
    ULIK_VURDERING("ULIK_VURDERING", "Ulik skjønnsvurdering"),
    PROSESSUELL_FEIL("PROSESSUELL_FEIL", "Saksbehandlingsfeil"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    private static final Map<String, AnkeOmgjørÅrsak> KODER = new LinkedHashMap<>();

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

    AnkeOmgjørÅrsak(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<AnkeOmgjørÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(AnkeOmgjørÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AnkeOmgjørÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static AnkeOmgjørÅrsak fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent AnkeOmgjørÅrsak: " + kode);
            }
            return ad;
        }

    }
}
