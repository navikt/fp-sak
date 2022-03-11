package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.time.LocalDate;
import java.util.List;

public record ArbeidOgInntektsmeldingDto(List<InntektsmeldingDto> inntektsmeldinger,
                                         List<ArbeidsforholdDto> arbeidsforhold,
                                         List<InntektDto> inntekter,
                                         LocalDate skjæringstidspunkt){}
