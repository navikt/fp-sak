package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum IverksettingStatus implements Kodeverdi {

    IKKE_IVERKSATT("IKKE_IVERKSATT", "Ikke iverksatt"),
    IVERKSATT("IVERKSATT", "Iverksatt"),

    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "IVERKSETTING_STATUS";
    private static final Map<String, IverksettingStatus> KODER = new LinkedHashMap<>();

    private String navn;

    @JsonValue
    private String kode;

    IverksettingStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, IverksettingStatus> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<IverksettingStatus, String> {
        @Override
        public String convertToDatabaseColumn(IverksettingStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public IverksettingStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static IverksettingStatus fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent IverksettingStatus: " + kode);
            }
            return ad;
        }

    }

}
