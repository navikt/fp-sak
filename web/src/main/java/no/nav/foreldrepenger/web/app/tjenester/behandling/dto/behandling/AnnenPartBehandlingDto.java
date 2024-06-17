package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;

import java.util.UUID;

public record AnnenPartBehandlingDto(String saksnummer, RelasjonsRolleType relasjonsRolleType, UUID behandlingUuid) {
}
