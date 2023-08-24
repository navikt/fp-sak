package no.nav.foreldrepenger.behandlingslager.behandling.innsyn;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum InnsynResultatType implements Kodeverdi {

    INNVILGET("INNV", "Innvilget innsyn"),
    DELVIS_INNVILGET("DELV", "Delvis innvilget innsyn"),
    AVVIST("AVVIST", "Avslått innsyn"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, InnsynResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "INNSYN_RESULTAT_TYPE";

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

    public static Map<String, InnsynResultatType> kodeMap() {
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
