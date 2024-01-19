package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.Collections;
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
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, Vedtaksbrev> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VEDTAKSBREV";

    private String navn;
    @JsonValue
    private String kode;

    Vedtaksbrev(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }


    public static Map<String, Vedtaksbrev> kodeMap() {
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
