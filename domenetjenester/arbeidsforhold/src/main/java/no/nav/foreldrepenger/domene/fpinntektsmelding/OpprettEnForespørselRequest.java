package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettEnForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                          @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                          @NotNull @Valid LocalDate skjæringstidspunkt,
                                          @NotNull @Valid FpinntektsmeldingYtelse ytelsetype,
                                          @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                          @Valid LocalDate førsteUttaksdato) {
}
