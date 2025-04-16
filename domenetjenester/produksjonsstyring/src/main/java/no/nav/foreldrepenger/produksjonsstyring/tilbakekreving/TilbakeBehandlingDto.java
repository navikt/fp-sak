package no.nav.foreldrepenger.produksjonsstyring.tilbakekreving;

import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

public record TilbakeBehandlingDto(Long id, UUID uuid, BehandlingType type)  {
}
