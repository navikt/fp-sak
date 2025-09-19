package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

public record BehandlingOpprettingDto(@NotNull BehandlingType behandlingType, @NotNull boolean kanOppretteBehandling) {
}
