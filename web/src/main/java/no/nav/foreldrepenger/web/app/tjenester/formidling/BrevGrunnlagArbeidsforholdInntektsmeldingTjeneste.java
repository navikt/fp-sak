package no.nav.foreldrepenger.web.app.tjenester.formidling;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.kontrakter.fpsak.inntektsmeldinger.ArbeidsforholdInntektsmeldingerDto;

public class BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste {

    private BrevGrunnlagArbeidsforholdInntektsmeldingTjeneste() {
        // Skjuler default konstruktør
    }

    /**
     * Én linje per arbeidsgiver. Arbeidsgiveren regnes som mottatt kun når alle arbeidsforhold som ikke er avklart har mottatt inntektsmelding.
     */
    public static ArbeidsforholdInntektsmeldingerDto mapInntektsmeldingStatus(List<ArbeidsforholdInntektsmeldingStatus> arbeidsforholdInntektsmeldingStatuser) {
        var erMottattPerArbeidsgiver = arbeidsforholdInntektsmeldingStatuser.stream()
            .filter(arb -> !arb.inntektsmeldingStatus().equals(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD))
            .collect(Collectors.toMap(ArbeidsforholdInntektsmeldingStatus::arbeidsgiver,
                arb -> arb.inntektsmeldingStatus().equals(ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT),
                Boolean::logicalAnd, LinkedHashMap::new));

        var inntektsmeldingerMedStatus = erMottattPerArbeidsgiver.entrySet().stream()
            .map(e -> new ArbeidsforholdInntektsmeldingerDto.ArbeidsforholdInntektsmeldingDto(e.getKey().getIdentifikator(), e.getValue()))
            .toList();
        return new ArbeidsforholdInntektsmeldingerDto(inntektsmeldingerMedStatus);
    }
}
