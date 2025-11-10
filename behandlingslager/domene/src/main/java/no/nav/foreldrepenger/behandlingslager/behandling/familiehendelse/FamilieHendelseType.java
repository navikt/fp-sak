package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum FamilieHendelseType implements Kodeverdi {

    ADOPSJON("ADPSJN", "Adopsjon"),
    OMSORG("OMSRGO", "Omsorgoverdragelse"),
    FØDSEL("FODSL", "Fødsel"),
    TERMIN("TERM", "Termin"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke satt eller valgt kode"),

    ;

    private static final Map<String, FamilieHendelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FAMILIE_HENDELSE_TYPE";

    private final String navn;

    @JsonValue
    private final String kode;

    FamilieHendelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, FamilieHendelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static boolean gjelderFødsel(FamilieHendelseType type) {
        return TERMIN.equals(type) || FØDSEL.equals(type);
    }

    public static boolean gjelderAdopsjon(FamilieHendelseType type) {
        return ADOPSJON.equals(type) || OMSORG.equals(type);
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
    public static class KodeverdiConverter implements AttributeConverter<FamilieHendelseType, String> {
        @Override
        public String convertToDatabaseColumn(FamilieHendelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FamilieHendelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static FamilieHendelseType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent FamilieHendelseType: " + kode);
            }
            return ad;
        }

    }
}
