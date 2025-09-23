package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InntektDto(@NotNull String arbeidsgiverIdent, List<InntektspostDto> inntekter) {}
