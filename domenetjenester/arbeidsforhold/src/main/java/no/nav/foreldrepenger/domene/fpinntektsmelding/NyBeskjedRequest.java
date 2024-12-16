package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record NyBeskjedRequest(@Valid @NotNull OrganisasjonsnummerDto orgnummer,
                               @NotNull @Valid SaksnummerDto fagsakSaksnummer) {
}
