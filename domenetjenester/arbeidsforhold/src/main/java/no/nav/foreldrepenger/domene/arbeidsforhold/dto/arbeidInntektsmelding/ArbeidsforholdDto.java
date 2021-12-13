package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArbeidsforholdDto(String arbeidsgiverIdent, String internArbeidsforholdId,
                               String eksternArbeidsforholdId,
                               LocalDate fom,
                               LocalDate tom,
                               BigDecimal stillingsprosent){}
