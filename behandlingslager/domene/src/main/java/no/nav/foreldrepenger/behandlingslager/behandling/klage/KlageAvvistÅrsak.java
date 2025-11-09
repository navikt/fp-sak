package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KlageAvvistÅrsak implements Kodeverdi {

    KLAGET_FOR_SENT("KLAGET_FOR_SENT", "Bruker har klaget for sent"),
    KLAGE_UGYLDIG("KLAGE_UGYLDIG", "Klagen er ugyldig"),
    IKKE_PAKLAGD_VEDTAK("IKKE_PAKLAGD_VEDTAK", "Ikke påklagd et vedtak"),
    KLAGER_IKKE_PART("KLAGER_IKKE_PART", "Klager er ikke part"),
    IKKE_KONKRET("IKKE_KONKRET", "Klagen er ikke konkret"),
    IKKE_SIGNERT("IKKE_SIGNERT", "Klagen er ikke signert"),
    UDEFINERT("-", "Ikke definert"),
    ;

    private static final Map<String, KlageAvvistÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_AVVIST_AARSAK";

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

    KlageAvvistÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, KlageAvvistÅrsak> kodeMap() {
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
