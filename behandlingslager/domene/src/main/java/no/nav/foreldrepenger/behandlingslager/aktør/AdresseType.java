package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
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

    @JsonIgnore
    private String navn;

    private String kode;
    @JsonIgnore
    private String offisiellKode;

    private AdresseType(String kode) {
        this.kode = kode;
    }

    private AdresseType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @JsonCreator
    public static AdresseType fraKode(@JsonProperty("kode") String kode) {
        var ad = fraKodeOptional(kode);
        if (ad.isEmpty()) {
            throw new IllegalArgumentException("Ukjent RelasjonsRolleType: " + kode);
        }
        return ad.get();
    }

    public static Map<String, AdresseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static Optional<AdresseType> fraKodeOptional(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(KODER.get(kode));
    }


    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
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

    }

    private static final Set<AdresseType> UTLANDSTYPER = Set.of(POSTADRESSE_UTLAND, MIDLERTIDIG_POSTADRESSE_UTLAND);

    public boolean erUtlandsAdresseType() {
        return UTLANDSTYPER.contains(this);
    }

}
