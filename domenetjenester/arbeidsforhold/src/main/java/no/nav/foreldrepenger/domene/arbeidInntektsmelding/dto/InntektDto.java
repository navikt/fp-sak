package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.util.List;

public record InntektDto(String arbeidsgiverIdent, List<InntektspostDto> inntekter) {}
