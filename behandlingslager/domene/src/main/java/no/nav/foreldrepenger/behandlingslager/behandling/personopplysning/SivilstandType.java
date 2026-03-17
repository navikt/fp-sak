package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum SivilstandType implements Kodeverdi, DatabaseKode {

    ENKEMANN("ENKE", "Enke/-mann"),
    GIFT("GIFT", "Gift"),
    GJENLEVENDE_PARTNER("GJPA", "Gjenlevende partner"),
    GIFT_ADSKILT("GLAD", "Gift, lever adskilt"),
    UOPPGITT("NULL", "Uoppgitt"),
    REGISTRERT_PARTNER("REPA", "Registrert partner"),
    SAMBOER("SAMB", "Samboer"),
    SEPARERT_PARTNER("SEPA", "Separert partner"),
    SEPARERT("SEPR", "Separert"),
    SKILT("SKIL", "Skilt"),
    SKILT_PARTNER("SKPA", "Skilt partner"),
    UGIFT("UGIF", "Ugift"),
    ;

    private static final Map<String, SivilstandType> KODER = new LinkedHashMap<>();

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

    SivilstandType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

}
