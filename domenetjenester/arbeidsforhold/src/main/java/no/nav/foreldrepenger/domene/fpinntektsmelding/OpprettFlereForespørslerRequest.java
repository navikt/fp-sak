package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettFlereForespørslerRequest(@NotNull @Valid OpprettForespørselRequest.AktørIdDto aktørId,
                                              @NotNull LocalDate skjæringstidspunkt,
                                              @NotNull OpprettForespørselRequest.YtelseType ytelsetype,
                                              @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                              @Valid LocalDate førsteUttaksdato,
                                              @NotNull List<@NotNull @Valid OrganisasjonsnummerDto> organisasjonsnumre) {
}
