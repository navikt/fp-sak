package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OverstyrInntektsmeldingRequest(@NotNull @Valid AktørIdDto aktorId,
                                             @NotNull @Valid ArbeidsgiverDto arbeidsgiverIdent,
                                             @NotNull LocalDate startdato,
                                             @NotNull YtelseType ytelse,
                                             @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal inntekt,
                                             @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal refusjon,
                                             @NotNull List<@Valid RefusjonendringRequestDto> refusjonsendringer,
                                             @NotNull List<@Valid BortfaltNaturalytelseRequestDto> bortfaltNaturalytelsePerioder,
                                             @NotNull String opprettetAv) {
    protected record AktørIdDto(@NotNull @JsonValue String id){}
    protected record ArbeidsgiverDto(@NotNull @JsonValue String ident){}
    protected enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }
    protected record RefusjonendringRequestDto(@NotNull LocalDate fom,
                                                 @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {}

    protected record BortfaltNaturalytelseRequestDto(@NotNull LocalDate fom,
                                                  LocalDate tom,
                                                  @NotNull NaturalYtelseType naturalytelsetype,
                                                  @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beløp) {}
}
