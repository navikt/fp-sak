package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettEnForespørselRequest(@NotNull @Valid OpprettForespørselRequest.AktørIdDto aktørId,
                                         @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                         @NotNull LocalDate skjæringstidspunkt,
                                         @NotNull OpprettForespørselRequest.YtelseType ytelsetype,
                                         @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                         @Valid LocalDate førsteUttaksdato) {
}
