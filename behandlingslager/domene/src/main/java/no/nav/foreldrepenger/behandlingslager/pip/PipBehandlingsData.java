package no.nav.foreldrepenger.behandlingslager.pip;

import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public record PipBehandlingsData(BehandlingStatus behandlingStatus, String ansvarligSaksbehandler, Long behandlingId, UUID behandlingUuid,
                                 FagsakStatus fagsakStatus, Saksnummer saksnummer) {

    public Optional<String> getAnsvarligSaksbehandler() {
        return Optional.ofNullable(ansvarligSaksbehandler);
    }
}
