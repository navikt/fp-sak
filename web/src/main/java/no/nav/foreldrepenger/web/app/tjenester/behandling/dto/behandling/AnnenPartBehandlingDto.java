package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.UUID;

import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

public record AnnenPartBehandlingDto(SaksnummerDto saksnr, UUID behandlingUuid) {
}
