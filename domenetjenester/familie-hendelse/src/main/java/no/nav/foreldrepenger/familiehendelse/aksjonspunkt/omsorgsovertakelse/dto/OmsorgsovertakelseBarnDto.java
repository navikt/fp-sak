package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OmsorgsovertakelseBarnDto(@NotNull LocalDate fødselsdato, @NotNull @Min(0) @Max(10) Integer barnNummer) {
}
