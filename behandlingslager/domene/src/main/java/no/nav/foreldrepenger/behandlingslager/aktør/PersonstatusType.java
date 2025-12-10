package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum PersonstatusType implements Kodeverdi {

    ADNR("ADNR", "D-nummer"),
    BOSA("BOSA", "Bosatt (f.reg)"),
    DØD("DØD", "Død"),
    FOSV("FOSV", "Forsvunnet"),
    FØDR("FØDR", "Fødselregistrert"),
    UREG("UREG", "Ikke bosatt (f.reg)"),
    UTPE("UTPE", "Opphørt"),
    UTVA("UTVA", "Utflyttet"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, PersonstatusType> FRA_FREG = Map.ofEntries(
        Map.entry("inaktiv", ADNR),
        Map.entry("midlertidig", ADNR),
        Map.entry("bosatt", PersonstatusType.BOSA),
        Map.entry("doed", PersonstatusType.DØD),
        Map.entry("forsvunnet", PersonstatusType.FOSV),
        Map.entry("foedselsregistrert", PersonstatusType.FØDR),
        Map.entry("opphoert", PersonstatusType.UTPE),
        Map.entry("utflyttet", UTVA),
        Map.entry("ikkeBosatt", UREG)
    );

    private static final Map<String, PersonstatusType> KODER = new LinkedHashMap<>();

    private String navn;

    @JsonValue
    private String kode;

    PersonstatusType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static boolean erDød(PersonstatusType personstatus) {
        return DØD.equals(personstatus);
    }

    @Override
    public String getNavn() {
        return navn;
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
    public static class KodeverdiConverter implements AttributeConverter<PersonstatusType, String> {
        @Override
        public String convertToDatabaseColumn(PersonstatusType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public PersonstatusType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static PersonstatusType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent PersonstatusType: " + kode);
            }
            return ad;
        }
    }

    public static PersonstatusType fraFregPersonstatus(String fregStatus) {
        return fregStatus != null ? FRA_FREG.getOrDefault(fregStatus, UDEFINERT) : UDEFINERT;
    }

}
