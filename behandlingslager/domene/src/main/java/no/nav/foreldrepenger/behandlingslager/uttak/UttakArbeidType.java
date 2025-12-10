package no.nav.foreldrepenger.behandlingslager.uttak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum UttakArbeidType implements Kodeverdi {

    ORDINÆRT_ARBEID("ORDINÆRT_ARBEID", "Ordinært arbeid"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    FRILANS("FRILANS", "Frilans"),
    ANNET("ANNET", "Annet"),
    ;
    private static final Map<String, UttakArbeidType> KODER = new LinkedHashMap<>();

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

    UttakArbeidType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<UttakArbeidType, String> {
        @Override
        public String convertToDatabaseColumn(UttakArbeidType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public UttakArbeidType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static UttakArbeidType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent UttakArbeidType: " + kode);
            }
            return ad;
        }
    }

    public boolean erArbeidstakerEllerFrilans() {
        return ORDINÆRT_ARBEID.equals(this) || FRILANS.equals(this);
    }
}
