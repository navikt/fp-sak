package no.nav.foreldrepenger.historikk;

/**
 * <p>
 * Definerer typer historikkinnslag for arbeidsforhold i 5080
 * </p>
 */
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
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

    @JsonIgnore
    private String navn;

    private String kode;

    private VurderArbeidsforholdHistorikkinnslag(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static VurderArbeidsforholdHistorikkinnslag fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VurderArbeidsforholdHistorikkinnslag: " + kode);
        }
        return ad;
    }

    public static Map<String, VurderArbeidsforholdHistorikkinnslag> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    
    @Override
    public String getOffisiellKode() {
        return getKode();
    }
}
