package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;

public record InntektspostDto (BigDecimal beløp, LocalDate fom, LocalDate tom, InntektspostType type){}
