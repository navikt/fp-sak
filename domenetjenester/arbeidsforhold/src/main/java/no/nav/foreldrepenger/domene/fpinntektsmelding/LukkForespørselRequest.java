package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record LukkForespørselRequest(@NotNull @Valid OpprettForespørselRequest.OrganisasjonsnummerDto orgnummer,
                                     @NotNull @Valid OpprettForespørselRequest.SaksnummerDto fagsakSaksnummer) {
}
