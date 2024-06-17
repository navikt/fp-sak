package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

public record TilbakeBehandlingDto(Long id, UUID uuid, BehandlingType type) {
}
