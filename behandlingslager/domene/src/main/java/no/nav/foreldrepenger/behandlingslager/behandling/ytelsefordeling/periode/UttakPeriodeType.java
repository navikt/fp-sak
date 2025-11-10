package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum UttakPeriodeType implements Kodeverdi {

    FELLESPERIODE("FELLESPERIODE", "Fellesperioden"),
    MØDREKVOTE("MØDREKVOTE", "Mødrekvoten"),
    FEDREKVOTE("FEDREKVOTE", "Fedrekvoten"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger"),
    FORELDREPENGER_FØR_FØDSEL("FORELDREPENGER_FØR_FØDSEL", "Foreldrepenger før fødsel"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke satt eller valgt kode"),
    ;
    private static final Map<String, UttakPeriodeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTTAK_PERIODE_TYPE";

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

    UttakPeriodeType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static UttakPeriodeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent UttakPeriodeType: " + kode);
        }
        return ad;
    }
    public static Map<String, UttakPeriodeType> kodeMap() {
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

    public boolean harAktivitetskrav() {
        return Set.of(FELLESPERIODE, FORELDREPENGER).contains(this);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<UttakPeriodeType, String> {
        @Override
        public String convertToDatabaseColumn(UttakPeriodeType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UttakPeriodeType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
