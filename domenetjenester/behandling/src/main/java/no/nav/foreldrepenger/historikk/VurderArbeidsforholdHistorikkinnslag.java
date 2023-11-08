package no.nav.foreldrepenger.historikk;

/**
 * <p>
 * Definerer typer historikkinnslag for arbeidsforhold i 5080
 * </p>
 */

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VurderArbeidsforholdHistorikkinnslag implements Kodeverdi {

    UDEFINERT("-", "UDEFINERT"),
    IKKE_BRUK("IKKE_BRUK", "Ikke bruk"),
    NYTT_ARBEIDSFORHOLD("NYTT_ARBEIDSFORHOLD", "Arbeidsforholdet er ansett som nytt"),
    SLÅTT_SAMMEN_MED_ANNET("SLÅTT_SAMMEN_MED_ANNET", "Arbeidsforholdet er slått sammen med annet"),
    MANGLENDE_OPPLYSNINGER("MANGLENDE_OPPLYSNINGER", "Benytt i behandlingen, men har manglende opplysninger"),
    LAGT_TIL_AV_SAKSBEHANDLER("LAGT_TIL_AV_SAKSBEHANDLER", "Arbeidsforholdet er lagt til av saksbehandler beregningsgrunnlaget"),
    INNTEKT_IKKE_MED_I_BG("INNTEKT_IKKE_MED_I_BG", "Benytt i behandlingen. Inntekten er ikke med i beregningsgrunnlaget"),
    BRUK_MED_OVERSTYRTE_PERIODER("BRUK_MED_OVERSTYRTE_PERIODER", "Bruk arbeidsforholdet med overstyrt periode"),
    BENYTT_A_INNTEKT_I_BG("BENYTT_A_INNTEKT_I_BG", "Benytt i behandlingen. Inntekt fra A-inntekt benyttes i beregningsgrunnlaget"),
    SØKER_ER_IKKE_I_PERMISJON("SØKER_ER_IKKE_I_PERMISJON", "Søker er ikke i permisjon"),
    SØKER_ER_I_PERMISJON("SØKER_ER_I_PERMISJON", "Søker er i permisjon"),
    ;

    private static final Map<String, VurderArbeidsforholdHistorikkinnslag> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VURDER_ARBEIDSFORHOLD_HISTORIKKINNSLAG";

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

    VurderArbeidsforholdHistorikkinnslag(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, VurderArbeidsforholdHistorikkinnslag> kodeMap() {
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
