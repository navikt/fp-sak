package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum UttakPeriodeVurderingType implements Kodeverdi {

    PERIODE_OK("PERIODE_OK", "Periode er OK"),
    PERIODE_OK_ENDRET("PERIODE_OK_ENDRET", "Periode er OK med endringer"),
    PERIODE_KAN_IKKE_AVKLARES("PERIODE_KAN_IKKE_AVKLARES", "Perioden kan ikke avklares"),
    PERIODE_IKKE_VURDERT("PERIODE_IKKE_VURDERT", "Perioden er ikke vurdert"),
    ;
    private static final Map<String, UttakPeriodeVurderingType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "UTTAK_PERIODE_VURDERING_TYPE";

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

    UttakPeriodeVurderingType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, UttakPeriodeVurderingType> kodeMap() {
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
    public static class KodeverdiConverter implements AttributeConverter<UttakPeriodeVurderingType, String> {
        @Override
        public String convertToDatabaseColumn(UttakPeriodeVurderingType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UttakPeriodeVurderingType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static UttakPeriodeVurderingType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent UttakPeriodeVurderingType: " + kode);
            }
            return ad;
        }
    }
}
