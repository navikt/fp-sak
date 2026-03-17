package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum AdresseType implements Kodeverdi, DatabaseKode {

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
    @EnumeratedValue
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

    private static final Set<AdresseType> UTLANDSTYPER = Set.of(POSTADRESSE_UTLAND, MIDLERTIDIG_POSTADRESSE_UTLAND);

    public boolean erUtlandsAdresseType() {
        return UTLANDSTYPER.contains(this);
    }

}
