package no.nav.foreldrepenger.domene.arbeidsforhold.dto.arbeidInntektsmelding;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InntektspostDto (BigDecimal beløp, LocalDate fom, LocalDate tom, InntektspostType type){}
