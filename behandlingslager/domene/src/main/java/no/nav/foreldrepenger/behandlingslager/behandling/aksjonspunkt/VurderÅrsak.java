package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VurderÅrsak implements Kodeverdi, DatabaseKode {

    FEIL_FAKTA("FEIL_FAKTA", "Fakta"),
    FEIL_LOV("FEIL_LOV", "Regel-/lovanvendelse"),
    SKJØNN("SKJØNN", "Skjønn"),
    UTREDNING("UTREDNING", "Utredning"),
    SAKSFLYT("SAKSFLYT", "Saksflyt"),
    BEGRUNNELSE("BEGRUNNELSE", "Begrunnelse"),

    @Deprecated
    ANNET("ANNET", "Annet"), // UTGÅTT, beholdes pga historikk
    @Deprecated
    FEIL_REGEL("FEIL_REGEL", "Feil regelforståelse"), // UTGÅTT, beholdes pga historikk
    ;

    private static final Map<String, VurderÅrsak> KODER = new LinkedHashMap<>();

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

    VurderÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static VurderÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VurderÅrsak: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

}
