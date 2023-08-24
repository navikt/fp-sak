package no.nav.foreldrepenger.web.server.abac;

import jakarta.validation.Valid;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.util.Set;

// Til bruk for evt abac attributefinders
public record PipDto(@Valid Set<AktørId> aktørIder, String fagsakStatus, String behandlingStatus) {
}
