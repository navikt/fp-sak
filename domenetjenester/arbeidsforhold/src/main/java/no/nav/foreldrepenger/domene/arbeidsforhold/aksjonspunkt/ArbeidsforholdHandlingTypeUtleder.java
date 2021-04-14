package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BRUK;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.IKKE_BRUK;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;

import org.apache.commons.lang3.StringUtils;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;

class ArbeidsforholdHandlingTypeUtleder {

    private ArbeidsforholdHandlingTypeUtleder() {
        // skjul public constructor
    }

    static ArbeidsforholdHandlingType utledHandling(ArbeidsforholdDto arbeidsforholdDto) {

        if (inntektSkalIkkeMedTilBeregningsgrunnlaget(arbeidsforholdDto)) {
            return INNTEKT_IKKE_MED_I_BG;
        }
        if (skalLeggeTilNyttArbeidsforhold(arbeidsforholdDto)) {
            return LAGT_TIL_AV_SAKSBEHANDLER;
        }
        if (skalLeggeTilNyttArbeidsforholdBasertPåInntektsmelding(arbeidsforholdDto)) {
            return BASERT_PÅ_INNTEKTSMELDING;
        }
        if (skalOverstyrePerioder(arbeidsforholdDto)) {
            return BRUK_MED_OVERSTYRT_PERIODE;
        }
        if (skalBrukeUtenInnteksmelding(arbeidsforholdDto)) {
            return BRUK_UTEN_INNTEKTSMELDING;
        }
        if (skalErstatteAnnenInntektsmelding(arbeidsforholdDto)) {
            return SLÅTT_SAMMEN_MED_ANNET;
        }
        if (erNyttArbeidsforhold(arbeidsforholdDto)) {
            return NYTT_ARBEIDSFORHOLD;
        }
        if (brukArbeidsforholdet(arbeidsforholdDto)) {
            return BRUK;
        }
        return IKKE_BRUK;
    }

    private static boolean skalOverstyrePerioder(ArbeidsforholdDto arbeidsforholdDto) {
        return (arbeidsforholdDto.getOverstyrtTom() != null)
                && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean inntektSkalIkkeMedTilBeregningsgrunnlaget(ArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.FALSE.equals(arbeidsforholdDto.getInntektMedTilBeregningsgrunnlag())
                && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean skalBrukeUtenInnteksmelding(ArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getFortsettBehandlingUtenInntektsmelding())
                && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean skalLeggeTilNyttArbeidsforhold(ArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getLagtTilAvSaksbehandler())
                && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean skalLeggeTilNyttArbeidsforholdBasertPåInntektsmelding(ArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getBasertPaInntektsmelding())
                && brukArbeidsforholdet(arbeidsforholdDto);
    }

    static boolean skalErstatteAnnenInntektsmelding(ArbeidsforholdDto arbeidsforholdDto) {
        return !StringUtils.isEmpty(arbeidsforholdDto.getErstatterArbeidsforholdId())
                && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static Boolean erNyttArbeidsforhold(ArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getErNyttArbeidsforhold())
                && brukArbeidsforholdet(arbeidsforholdDto);
    }

    private static boolean brukArbeidsforholdet(ArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getBrukArbeidsforholdet());
    }

}
