package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

/**
 * <p>
 * Definerer statuser for bekreftet permisjoner
 * </p>
 */

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum BekreftetPermisjonStatus implements Kodeverdi {

    UDEFINERT("-", "UDEFINERT"),
    BRUK_PERMISJON("BRUK_PERMISJON", "Bruk permisjonen til arbeidsforholdet"),
    IKKE_BRUK_PERMISJON("IKKE_BRUK_PERMISJON", "Ikke bruk permisjonen til arbeidsforholdet"),
    UGYLDIGE_PERIODER("UGYLDIGE_PERIODER", "Arbeidsforholdet inneholder permisjoner med ugyldige perioder"),
    ;

    private static final Map<String, BekreftetPermisjonStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEKREFTET_PERMISJON_STATUS";

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

    BekreftetPermisjonStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BekreftetPermisjonStatus fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BekreftPermisjonStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, BekreftetPermisjonStatus> kodeMap() {
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
}
