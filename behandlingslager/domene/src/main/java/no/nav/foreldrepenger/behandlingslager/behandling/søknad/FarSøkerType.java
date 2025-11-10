package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum FarSøkerType implements Kodeverdi {
    ADOPTERER_ALENE("ADOPTERER_ALENE", "Adopterer barnet eller barna alene"),
    ANDRE_FORELDER_DØD("ANDRE_FORELDER_DØD", "Den andre forelderen er død"),
    OVERTATT_OMSORG("OVERTATT_OMSORG", "Overtatt omsorg < 56 uker"),
    OVERTATT_OMSORG_F("OVERTATT_OMSORG_F", "Overtatt omsorg fødsel"),
    ANDRE_FORELD_DØD_F("ANDRE_FORELD_DØD_F", "Overtatt omsorg ifm. død ved fødsel"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    public static final String KODEVERK = "FAR_SOEKER_TYPE";

    private static final Map<String, FarSøkerType> KODER = new LinkedHashMap<>();

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

    FarSøkerType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static FarSøkerType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FarSøkerType: " + kode);
        }
        return ad;
    }

    public static Map<String, FarSøkerType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<FarSøkerType, String> {
        @Override
        public String convertToDatabaseColumn(FarSøkerType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FarSøkerType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
