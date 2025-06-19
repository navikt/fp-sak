package no.nav.foreldrepenger.web.app.tjenester.vedtak.ekstern;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record YtelseInfoEksternRequest(@Valid @NotNull @Pattern(regexp = "\\d{11}|\\d{13}") String ident,
                                       @Valid @NotNull LocalDate fom,
                                       @Valid LocalDate tom) {

}
