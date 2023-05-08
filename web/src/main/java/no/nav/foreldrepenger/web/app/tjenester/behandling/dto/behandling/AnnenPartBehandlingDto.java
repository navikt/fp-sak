package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;

public record AnnenPartBehandlingDto(String saksnummer, RelasjonsRolleType relasjonsRolleType, UUID behandlingUuid) {
}
