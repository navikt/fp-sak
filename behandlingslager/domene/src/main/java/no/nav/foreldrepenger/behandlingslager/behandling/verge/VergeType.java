package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VergeType implements Kodeverdi {

    BARN("BARN", "Verge for barn under 18 år"),
    FBARN("FBARN", "Verge for foreldreløst barn under 18 år"),
    VOKSEN("VOKSEN", "Verge for voksen"),
    ADVOKAT("ADVOKAT", "Advokat/advokatfullmektig"),
    ANNEN_F("ANNEN_F", "Annen fullmektig"),
    ;

    private static final Map<String, VergeType> KODER = new LinkedHashMap<>();

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

    VergeType(String kode, String navn) {
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


}
