package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum BeregningsgrunnlagRegelType implements Kodeverdi {

    SKJÆRINGSTIDSPUNKT("SKJÆRINGSTIDSPUNKT", "Fastsette skjæringstidspunkt"),
    BRUKERS_STATUS("BRUKERS_STATUS", "Fastsette brukers status/aktivitetstatus"),
    // Skal ikke lagres til men eksisterer fordi det finnes entries med denne i databasen (før ble det kun lagret 1 sporing for periodisering)
    @Deprecated
    PERIODISERING("PERIODISERING", "Periodiser beregningsgrunnlag"),

    PERIODISERING_NATURALYTELSE("PERIODISERING_NATURALYTELSE", "Periodiser beregningsgrunnlag pga naturalytelse"),
    PERIODISERING_REFUSJON("PERIODISERING_REFUSJON", "Periodiser beregningsgrunnlag pga refusjon"),
    PERIODISERING_GRADERING("PERIODISERING_GRADERING", "Periodiser beregningsgrunnlag pga gradering og endring i utbetalingsgrad"),
    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "BG_REGEL_TYPE";

    private static final Map<String, BeregningsgrunnlagRegelType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;

    @JsonValue
    private String kode;

    BeregningsgrunnlagRegelType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BeregningsgrunnlagRegelType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningsgrunnlagRegelType: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningsgrunnlagRegelType> kodeMap() {
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
