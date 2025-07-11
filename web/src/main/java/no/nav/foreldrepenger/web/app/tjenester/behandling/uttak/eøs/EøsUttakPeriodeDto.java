package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public record EøsUttakPeriodeDto(@NotNull LocalDate fom,
                                 @NotNull LocalDate tom,
                                 @NotNull @Min(0) @Max(1000) @Digits(integer = 3, fraction = 2) BigDecimal trekkdager,
                                 @NotNull @ValidKodeverk UttakPeriodeType trekkonto) {
}
