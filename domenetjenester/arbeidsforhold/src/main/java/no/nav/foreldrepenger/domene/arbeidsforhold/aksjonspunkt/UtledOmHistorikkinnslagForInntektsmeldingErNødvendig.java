package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import java.time.LocalDate;
import java.util.Optional;

final class UtledOmHistorikkinnslagForInntektsmeldingErNødvendig {

    private UtledOmHistorikkinnslagForInntektsmeldingErNødvendig() {
        // Skjul default constructor
    }

    static boolean utled(ArbeidsforholdDto arbeidsforholdDto, Optional<LocalDate> stpOpt) {
        if (harMottattInntektsmelding(arbeidsforholdDto)) {
            return false;
        }
        if (skalBrukePermisjon(arbeidsforholdDto)) {
            return false;
        }
        return !arbeidsforholdetStarterPåEllerEtterStp(arbeidsforholdDto, stpOpt);
    }

    private static boolean harMottattInntektsmelding(ArbeidsforholdDto arbeidsforholdDto) {
        return arbeidsforholdDto.getMottattDatoInntektsmelding() != null;
    }

    private static boolean skalBrukePermisjon(ArbeidsforholdDto arbeidsforholdDto) {
        return Boolean.TRUE.equals(arbeidsforholdDto.getBrukPermisjon());
    }

    private static boolean arbeidsforholdetStarterPåEllerEtterStp(ArbeidsforholdDto arbeidsforholdDto, Optional<LocalDate> stpOpt) {
        if (stpOpt.isPresent()) {
            final LocalDate stp = stpOpt.get();
            return arbeidsforholdDto.getFomDato().isEqual(stp) || arbeidsforholdDto.getFomDato().isAfter(stp);
        }
        return false;
    }

}
