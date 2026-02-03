package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;

public record NaturalYtelseDto(@NotNull Periode periode,
                               @NotNull Beløp beloepPerMnd,
                               @NotNull NaturalYtelseType type,
                               @NotNull String indexKey) {

    public record Periode(@NotNull LocalDate fomDato, @NotNull LocalDate tomDato) { }
    public record Beløp(@NotNull BigDecimal verdi) { }
}
