package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import java.util.List;

public record InntektDto(String arbeidsgiverIdent, List<InntektspostDto> inntekter) {}
