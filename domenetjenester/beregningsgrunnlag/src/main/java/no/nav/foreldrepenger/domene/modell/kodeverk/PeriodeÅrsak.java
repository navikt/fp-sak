package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum PeriodeÅrsak implements Kodeverdi {

    NATURALYTELSE_BORTFALT("NATURALYTELSE_BORTFALT", "Naturalytelse bortfalt"),
    ARBEIDSFORHOLD_AVSLUTTET("ARBEIDSFORHOLD_AVSLUTTET", "Arbeidsforhold avsluttet"),
    NATURALYTELSE_TILKOMMER("NATURALYTELSE_TILKOMMER", "Naturalytelse tilkommer"),
    ENDRING_I_REFUSJONSKRAV("ENDRING_I_REFUSJONSKRAV", "Endring i refusjonskrav"),
    REFUSJON_OPPHØRER("REFUSJON_OPPHØRER", "Refusjon opphører"),
    GRADERING("GRADERING", "Gradering"),
    GRADERING_OPPHØRER("GRADERING_OPPHØRER", "Gradering opphører"),
    ENDRING_I_AKTIVITETER_SØKT_FOR("ENDRING_I_AKTIVITETER_SØKT_FOR", "Endring i aktiviteter søkt for"),
    REFUSJON_AVSLÅTT("REFUSJON_AVSLÅTT", "Refusjon avslått"),

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    ;

    private static final Map<String, PeriodeÅrsak> KODER = new LinkedHashMap<>();

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

    PeriodeÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static PeriodeÅrsak fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PeriodeÅrsak: " + kode);
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
