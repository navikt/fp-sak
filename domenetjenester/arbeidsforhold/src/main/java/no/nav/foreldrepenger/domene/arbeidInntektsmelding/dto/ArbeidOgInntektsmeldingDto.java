package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ArbeidOgInntektsmeldingDto(@NotNull List<InntektsmeldingDto> inntektsmeldinger,
                                         @NotNull List<ArbeidsforholdDto> arbeidsforhold,
                                         @NotNull List<InntektDto> inntekter,
                                         @NotNull LocalDate skjæringstidspunkt){}
