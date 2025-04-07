package no.nav.foreldrepenger.behandlingslager.pip;

import java.util.Optional;
import java.util.UUID;

public record PipBehandlingsData(String behandligStatus, String ansvarligSaksbehandler, Long behandlingId, UUID behandlingUuid,
                                 String fagsakStatus, String saksnummer) {

    public Optional<String> getAnsvarligSaksbehandler() {
        return Optional.ofNullable(ansvarligSaksbehandler);
    }
}
