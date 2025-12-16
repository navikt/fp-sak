package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum RelatertYtelseTilstand implements Kodeverdi {

    ÅPEN("ÅPEN", "Åpen"),
    LØPENDE("LØPENDE", "Løpende"),
    AVSLUTTET("AVSLUTTET", "Avsluttet"),
    IKKE_STARTET("IKKESTARTET", "Ikke startet"),
    ;

    private static final Map<String, RelatertYtelseTilstand> KODER = new LinkedHashMap<>();

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

    RelatertYtelseTilstand(String kode, String navn) {
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
