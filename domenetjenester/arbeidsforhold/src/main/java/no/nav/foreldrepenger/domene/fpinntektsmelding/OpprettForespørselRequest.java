package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull FpinntektsmeldingYtelse ytelsetype,
                                        @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                        @Valid LocalDate førsteUttaksdato,
                                        List<@Valid OrganisasjonsnummerDto> organisasjonsnumre) {
}
