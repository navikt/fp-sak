package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettKomplettForespørslerRequest(@NotNull @Valid AktørIdDto aktørId,
                                                @NotNull @Valid LocalDate skjæringstidspunkt,
                                                @NotNull @Valid FpInntektsmeldingYtelse ytelsetype,
                                                @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                                @Valid LocalDate førsteUttaksdato,
                                                @NotNull List<@NotNull @Valid OrganisasjonsnummerDto> organisasjonsnummer) {
}
