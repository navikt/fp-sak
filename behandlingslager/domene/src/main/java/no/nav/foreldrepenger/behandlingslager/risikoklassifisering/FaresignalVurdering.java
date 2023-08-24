package no.nav.foreldrepenger.behandlingslager.risikoklassifisering;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum FaresignalVurdering implements Kodeverdi {

    INNVIRKNING("INNVIRKNING", "Faresignalene vurderes som reelle"),
    INNVILGET_REDUSERT("INNVILGET_REDUSERT", "Saken er innvilget med redusert beregningsgrunnlag"),
    INNVILGET_UENDRET("INNVILGET_UENDRET", "Saken er innvilget uten at faresignalene påvirket utfallet"),
    AVSLAG_FARESIGNAL("AVSLAG_FARESIGNAL", "Saken er avslått på grunn av faresignalene"),
    AVSLAG_ANNET("AVSLAG_ANNET", "Saken er avslått av andre årsaker"),
    INGEN_INNVIRKNING("INGEN_INNVIRKNING", "Faresignalene vurderes ikke som reelle"),
    UDEFINERT("-", "Udefinert"),
    ;

    private static final Map<String, FaresignalVurdering> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FARESIGNAL_VURDERING";

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

    FaresignalVurdering(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, FaresignalVurdering> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<FaresignalVurdering, String> {
        @Override
        public String convertToDatabaseColumn(FaresignalVurdering attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FaresignalVurdering convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static FaresignalVurdering fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent FaresignalVurdering: " + kode);
            }
            return ad;
        }
    }
}
