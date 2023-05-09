package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum RelasjonsRolleType implements Kodeverdi {

    EKTE("EKTE", "Ektefelle"),
    BARN("BARN", "Barn"),
    FARA("FARA", "Far"),
    MORA("MORA", "Mor"),
    REGISTRERT_PARTNER("REPA", "Registrert partner"),
    MEDMOR("MMOR", "Medmor"),

    // Mulig verdi i PO_RELASJON
    ANNEN_PART_FRA_SØKNAD("ANPA", "Annen part fra søknad"),

    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, RelasjonsRolleType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "RELASJONSROLLE_TYPE";

    private static final Set<RelasjonsRolleType> FORELDRE_ROLLER = Set.of(RelasjonsRolleType.MORA, RelasjonsRolleType.FARA, RelasjonsRolleType.MEDMOR);

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

    RelasjonsRolleType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static RelasjonsRolleType fraKode(String kode) {
        return Optional.ofNullable(kode).map(KODER::get)
            .orElseThrow(() -> new IllegalArgumentException("Ukjent RelasjonsRolleType: " + kode));
    }

    public static Map<String, RelasjonsRolleType> kodeMap() {
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

    public static boolean erFar(RelasjonsRolleType relasjon) {
        return FARA.getKode().equals(relasjon.getKode());
    }

    public static boolean erMedmor(RelasjonsRolleType relasjon) {
        return MEDMOR.equals(relasjon);
    }

    public static boolean erFarEllerMedmor(RelasjonsRolleType relasjon) {
        return erFar(relasjon) || erMedmor(relasjon);
    }

    public static boolean erMor(RelasjonsRolleType relasjon) {
        return MORA.getKode().equals(relasjon.getKode());
    }

    public static boolean erRegistrertForeldre(RelasjonsRolleType type) {
        return FORELDRE_ROLLER.contains(type);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<RelasjonsRolleType, String> {
        @Override
        public String convertToDatabaseColumn(RelasjonsRolleType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public RelasjonsRolleType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
