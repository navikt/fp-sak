package no.nav.foreldrepenger.behandlingslager.uttak;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum PeriodeResultatType implements Kodeverdi {

    INNVILGET("INNVILGET", "Innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    MANUELL_BEHANDLING("MANUELL_BEHANDLING", "Til manuell behandling"),
    ;

    private static final Map<String, PeriodeResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERIODE_RESULTAT_TYPE";

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

    PeriodeResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, PeriodeResultatType> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<PeriodeResultatType, String> {
        @Override
        public String convertToDatabaseColumn(PeriodeResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public PeriodeResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static PeriodeResultatType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent PeriodeResultatType: " + kode);
            }
            return ad;
        }
    }
}
