package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum PersonstatusType implements Kodeverdi {

    ADNR("ADNR", "Aktivt D-nummer", false),
    BOSA("BOSA", "Bosatt", true),
    DØD("DØD", "Død", true),
    FOSV("FOSV", "Forsvunnet/savnet", false),
    FØDR("FØDR", "Fødselregistrert", false),
    UREG("UREG", "Uregistrert person", false),
    UTAN("UTAN", "Utgått person annullert tilgang Fnr", false),
    UTPE("UTPE", "Utgått person", false),
    UTVA("UTVA", "Utvandret", true),
    UDEFINERT("-", "Ikke definert", false),
    ;

    private static final Map<String, PersonstatusType> FRA_FREG = Map.ofEntries(Map.entry("inaktiv", ADNR), Map.entry("midlertidig", ADNR),
        Map.entry("bosatt", PersonstatusType.BOSA), Map.entry("doed", PersonstatusType.DØD), Map.entry("forsvunnet", PersonstatusType.FOSV),
        Map.entry("foedselsregistrert", PersonstatusType.FØDR), Map.entry("opphoert", PersonstatusType.UTPE), Map.entry("utflyttet", UTVA),
        Map.entry("ikkeBosatt", UREG));

    private static final Map<String, PersonstatusType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERSONSTATUS_TYPE";

    private String navn;

    @JsonValue
    private String kode;

    private boolean fortsettBehandling;

    PersonstatusType(String kode, String navn, boolean fortsettBehandling) {
        this.kode = kode;
        this.navn = navn;
        this.fortsettBehandling = fortsettBehandling;
    }

    public static boolean erDød(PersonstatusType personstatus) {
        return DØD.equals(personstatus);
    }


    public static Map<String, PersonstatusType> kodeMap() {
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

    public static Set<PersonstatusType> personstatusTyperFortsattBehandling() {
        return Stream.of(values()).filter(s -> s.fortsettBehandling).collect(Collectors.toSet());
    }

    public static PersonstatusType fraFregPersonstatus(String fregStatus) {
        return fregStatus != null ? FRA_FREG.getOrDefault(fregStatus, UDEFINERT) : UDEFINERT;
    }

}
