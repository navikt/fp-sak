package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum VedtakResultatType implements Kodeverdi {

    INNVILGET("INNVILGET", "Innvilget"),
    AVSLAG("AVSLAG", "Avslag"),
    OPPHØR("OPPHØR", "Opphør"),
    VEDTAK_I_KLAGEBEHANDLING("VEDTAK_I_KLAGEBEHANDLING", "vedtak i klagebehandling"),
    VEDTAK_I_ANKEBEHANDLING("VEDTAK_I_ANKEBEHANDLING", "vedtak i ankebehandling"),
    VEDTAK_I_INNSYNBEHANDLING("VEDTAK_I_INNSYNBEHANDLING", "vedtak i innsynbehandling"),
    UDEFINERT("-", "Ikke definert"),

    ;

    private static final Map<String, VedtakResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VEDTAK_RESULTAT_TYPE";

    private final String navn;

    @JsonValue
    private final String kode;

    VedtakResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, VedtakResultatType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<VedtakResultatType, String> {
        @Override
        public String convertToDatabaseColumn(VedtakResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VedtakResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static VedtakResultatType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent VedtakResultatType: " + kode);
            }
            return ad;
        }
    }
}
