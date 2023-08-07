package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum AdresseType implements Kodeverdi, MedOffisiellKode {

    BOSTEDSADRESSE("BOSTEDSADRESSE", "Bostedsadresse", "BOAD"),
    POSTADRESSE("POSTADRESSE", "Postadresse", "POST"),
    POSTADRESSE_UTLAND("POSTADRESSE_UTLAND", "Postadresse i utlandet", "PUTL"),
    MIDLERTIDIG_POSTADRESSE_NORGE("MIDLERTIDIG_POSTADRESSE_NORGE", "Midlertidig postadresse i Norge", "TIAD"),
    MIDLERTIDIG_POSTADRESSE_UTLAND("MIDLERTIDIG_POSTADRESSE_UTLAND", "Midlertidig postadresse i utlandet", "UTAD"),
    UKJENT_ADRESSE("UKJENT_ADRESSE", "Ukjent adresse", "UKJE"),
    ;

    private static final Map<String, AdresseType> KODER = new LinkedHashMap<>();

    private static final String KODEVERK = "ADRESSE_TYPE";

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

    private final String offisiellKode;

    AdresseType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static Map<String, AdresseType> kodeMap() {
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
    public String getOffisiellKode() {
        return offisiellKode;
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
