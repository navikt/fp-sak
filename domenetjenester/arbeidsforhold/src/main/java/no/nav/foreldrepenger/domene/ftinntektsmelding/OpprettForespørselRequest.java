package no.nav.foreldrepenger.domene.ftinntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull YtelseType ytelseType,
                                        @NotNull @Valid SaksnummerDto saksnummer) {
    protected record AktørIdDto(@NotNull String id){}
    protected record SaksnummerDto(@NotNull String saksnr){}
    protected record OrganisasjonsnummerDto(@NotNull String orgnr){}
    protected enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }
}
