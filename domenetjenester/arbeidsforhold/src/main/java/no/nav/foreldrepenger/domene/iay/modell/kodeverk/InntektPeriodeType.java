package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.time.Period;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum InntektPeriodeType implements Kodeverdi {

    DAGLIG("DAGLG", "Daglig", Period.ofDays(1)),
    UKENTLIG("UKNLG", "Ukentlig", Period.ofWeeks(1)),
    BIUKENTLIG("14DLG", "Fjorten-daglig", Period.ofWeeks(2)),
    MÅNEDLIG("MNDLG", "Månedlig", Period.ofMonths(1)),
    ÅRLIG("AARLG", "Årlig", Period.ofYears(1)),
    FASTSATT25PAVVIK("INNFS", "Fastsatt etter 25 prosent avvik", Period.ofYears(1)),
    PREMIEGRUNNLAG("PREMGR", "Premiegrunnlag", Period.ofYears(1)),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert", null),
    ;

    private static final Map<String, InntektPeriodeType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "INNTEKT_PERIODE_TYPE";

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
    private final Period periode;

    InntektPeriodeType(String kode, String navn, Period periode) {
        this.kode = kode;
        this.navn = navn;
        this.periode = periode;
    }

    public static InntektPeriodeType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent InntektPeriodeType: " + kode);
        }
        return ad;
    }

    public static Map<String, InntektPeriodeType> kodeMap() {
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

    public Period getPeriode() {
        return periode;
    }

}
