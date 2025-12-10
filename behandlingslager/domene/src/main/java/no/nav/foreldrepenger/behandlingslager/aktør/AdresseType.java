package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum AdresseType implements Kodeverdi {

    BOSTEDSADRESSE("BOSTEDSADRESSE", "Bostedsadresse"),
    BOSTEDSADRESSE_UTLAND("BOSTEDSADRESSE_UTLAND", "Bostedsadresse utland"),
    POSTADRESSE("POSTADRESSE", "Kontaktadresse"),
    POSTADRESSE_UTLAND("POSTADRESSE_UTLAND", "Kontaktadresse utland"),
    MIDLERTIDIG_POSTADRESSE_NORGE("MIDLERTIDIG_POSTADRESSE_NORGE", "Oppholdsadresse"),
    MIDLERTIDIG_POSTADRESSE_UTLAND("MIDLERTIDIG_POSTADRESSE_UTLAND", "Oppholdsadresse utland"),
    UKJENT_ADRESSE("UKJENT_ADRESSE", "Ukjent adresse"),
    ;

    private static final Map<String, AdresseType> KODER = new LinkedHashMap<>();

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

    AdresseType(String kode, String navn) {
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
    public static class KodeverdiConverter implements AttributeConverter<AdresseType, String> {
        @Override
        public String convertToDatabaseColumn(AdresseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AdresseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static AdresseType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent RelasjonsRolleType: " + kode);
            }
            return ad;
        }
    }

    private static final Set<AdresseType> UTLANDSTYPER = Set.of(POSTADRESSE_UTLAND, MIDLERTIDIG_POSTADRESSE_UTLAND);

    public boolean erUtlandsAdresseType() {
        return UTLANDSTYPER.contains(this);
    }

}
