package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

public record BehandlingOpprettingDto(BehandlingType behandlingType, boolean kanOppretteBehandling) {
}
