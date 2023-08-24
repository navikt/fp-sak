package no.nav.foreldrepenger.økonomi.tilbakekreving.klient;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

import java.util.UUID;

public record TilbakeBehandlingDto(Long id, UUID uuid, BehandlingType type)  {
}
