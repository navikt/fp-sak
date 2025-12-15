package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Vedtaksbrev implements Kodeverdi {

    AUTOMATISK("AUTOMATISK", "Automatisk generert vedtaksbrev"),
    FRITEKST("FRITEKST", "Fritekstbrev"),
    INGEN("INGEN", "Ingen vedtaksbrev"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    private static final Map<String, Vedtaksbrev> KODER = new LinkedHashMap<>();

    private String navn;
    @JsonValue
    private String kode;

    Vedtaksbrev(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<Vedtaksbrev, String> {
        @Override
        public String convertToDatabaseColumn(Vedtaksbrev attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Vedtaksbrev convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static Vedtaksbrev fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent Vedtaksbrev: " + kode);
            }
            return ad;
        }
    }

}
