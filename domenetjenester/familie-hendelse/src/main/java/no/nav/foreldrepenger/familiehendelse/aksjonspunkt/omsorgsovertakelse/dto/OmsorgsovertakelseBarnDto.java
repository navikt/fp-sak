package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OmsorgsovertakelseBarnDto(@NotNull LocalDate fødselsdato, @NotNull Integer barnNummer) {
}
