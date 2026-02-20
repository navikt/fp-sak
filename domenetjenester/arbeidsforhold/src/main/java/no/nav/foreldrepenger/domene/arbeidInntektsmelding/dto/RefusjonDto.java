package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record RefusjonDto(@NotNull LocalDate fom,
                          @NotNull Beløp refusjonsbeløpMnd,
                          @NotNull String indexKey) {
    public record Beløp(@NotNull BigDecimal verdi) { }
}
