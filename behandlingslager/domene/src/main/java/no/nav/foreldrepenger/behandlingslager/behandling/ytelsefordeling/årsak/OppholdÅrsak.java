package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;


public enum OppholdÅrsak implements Årsak {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke satt eller valgt kode"),
    MØDREKVOTE_ANNEN_FORELDER("UTTAK_MØDREKVOTE_ANNEN_FORELDER", "Annen forelder har uttak av Mødrekvote"),
    FEDREKVOTE_ANNEN_FORELDER("UTTAK_FEDREKVOTE_ANNEN_FORELDER", "Annen forelder har uttak av Fedrekvote"),
    KVOTE_FELLESPERIODE_ANNEN_FORELDER("UTTAK_FELLESP_ANNEN_FORELDER", "Annen forelder har uttak av Fellesperiode"),
    KVOTE_FORELDREPENGER_ANNEN_FORELDER("UTTAK_FORELDREPENGER_ANNEN_FORELDER", "Annen forelder har uttak av Foreldrepenger"),
    ;
    private static final Map<String, OppholdÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "OPPHOLD_AARSAK_TYPE";

    public static final String DISKRIMINATOR = "OPPHOLD_AARSAK_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    OppholdÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static OppholdÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OppholdÅrsak: " + kode);
        }
        return ad;
    }
    public static Map<String, OppholdÅrsak> kodeMap() {
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

    @Override
    public String getDiskriminator() {
        return DISKRIMINATOR;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<OppholdÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(OppholdÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public OppholdÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
