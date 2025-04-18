package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record LukkForespørselRequest(@Valid OrganisasjonsnummerDto orgnummer,
                                     @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
