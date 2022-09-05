package no.nav.foreldrepenger.web.server.abac;

import java.util.Set;

import javax.validation.Valid;

import no.nav.foreldrepenger.domene.typer.AktørId;

public record AbacPipDto(@Valid Set<AktørId> aktørIder, AbacFagsakStatus fagsakStatus, AbacBehandlingStatus behandlingStatus) {
}
