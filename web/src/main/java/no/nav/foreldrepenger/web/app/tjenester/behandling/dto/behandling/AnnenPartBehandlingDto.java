package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;

public record AnnenPartBehandlingDto(@NotNull String saksnummer, @NotNull RelasjonsRolleType relasjonsRolleType, @NotNull UUID behandlingUuid) {
}
