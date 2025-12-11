package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

/**
 * <h3>Internt kodeverk</h3>
 * Definerer typer av handlinger en saksbehandler kan gjøre vedrørende et arbeidsforhold
 * <p>
 */

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum ArbeidsforholdHandlingType implements Kodeverdi {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    BRUK("BRUK", "Bruk"),
    NYTT_ARBEIDSFORHOLD("NYTT_ARBEIDSFORHOLD", "Arbeidsforholdet er ansett som nytt"),
    BRUK_UTEN_INNTEKTSMELDING("BRUK_UTEN_INNTEKTSMELDING", "Bruk, men ikke benytt inntektsmelding"),
    IKKE_BRUK("IKKE_BRUK", "Ikke bruk"),
    SLÅTT_SAMMEN_MED_ANNET("SLÅTT_SAMMEN_MED_ANNET", "Arbeidsforholdet er slått sammen med et annet"),
    LAGT_TIL_AV_SAKSBEHANDLER("LAGT_TIL_AV_SAKSBEHANDLER", "Arbeidsforhold lagt til av saksbehandler"),
    BASERT_PÅ_INNTEKTSMELDING("BASERT_PÅ_INNTEKTSMELDING", "Arbeidsforholdet som ikke ligger i Aa-reg er basert på inntektsmelding"),
    BRUK_MED_OVERSTYRT_PERIODE("BRUK_MED_OVERSTYRT_PERIODE", "Bruk arbeidsforholdet med overstyrt periode"),
    INNTEKT_IKKE_MED_I_BG("INNTEKT_IKKE_MED_I_BG", "Inntekten til arbeidsforholdet skal ikke være med i beregningsgrunnlaget"),
    ;

    private static final Set<ArbeidsforholdHandlingType> MED_OVERSTYRT_PERIODE = Set.of(BRUK_MED_OVERSTYRT_PERIODE, BASERT_PÅ_INNTEKTSMELDING,
            LAGT_TIL_AV_SAKSBEHANDLER);

    private static final Map<String, ArbeidsforholdHandlingType> KODER = new LinkedHashMap<>();

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

    ArbeidsforholdHandlingType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static ArbeidsforholdHandlingType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent ArbeidsforholdHandlingType: " + kode);
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

    public boolean erPeriodeOverstyrt() {
        return MED_OVERSTYRT_PERIODE.contains(this);
    }

}
