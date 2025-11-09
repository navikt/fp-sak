package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum BeregningsgrunnlagPeriodeRegelType implements Kodeverdi {
    FORESLÅ("FORESLÅ", "Foreslå beregningsgrunnlag"),
    FORESLÅ_2("FORESLÅ_2", "Foreslå beregningsgrunnlag del 2"),
    VILKÅR_VURDERING("VILKÅR_VURDERING", "Vurder beregningsvilkår"),
    FORDEL("FORDEL", "Fordel beregningsgrunnlag"),
    FASTSETT("FASTSETT", "Fastsett/fullføre beregningsgrunnlag"),
    OPPDATER_GRUNNLAG_SVP("OPPDATER_GRUNNLAG_SVP", "Oppdater grunnlag for SVP"),
    FASTSETT2("FASTSETT2", "Fastsette/fullføre beregningsgrunnlag for andre gangs kjøring for SVP"),
    FINN_GRENSEVERDI("FINN_GRENSEVERDI", "Finne grenseverdi til kjøring av fastsett beregningsgrunnlag for SVP"),
    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "BG_PERIODE_REGEL_TYPE";

    private static final Map<String, BeregningsgrunnlagPeriodeRegelType> KODER = new LinkedHashMap<>();

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

    BeregningsgrunnlagPeriodeRegelType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BeregningsgrunnlagPeriodeRegelType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningsgrunnlagPeriodeRegelType: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningsgrunnlagPeriodeRegelType> kodeMap() {
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
