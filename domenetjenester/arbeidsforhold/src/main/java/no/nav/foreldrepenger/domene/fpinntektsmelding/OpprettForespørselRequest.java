package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @NotNull @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull YtelseType ytelsetype,
                                        @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
    protected record AktørIdDto(@NotNull @JsonValue String id){}
    protected record SaksnummerDto(@NotNull @JsonValue String saksnr){}
    protected record OrganisasjonsnummerDto(@NotNull @JsonValue String orgnr){}
    protected enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }
}
