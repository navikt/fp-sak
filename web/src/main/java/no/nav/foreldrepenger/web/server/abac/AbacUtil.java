package no.nav.foreldrepenger.web.server.abac;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipBehandlingStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipFagsakStatus;

public final class AbacUtil {

    public static Optional<PipFagsakStatus> oversettFagstatus(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        if (kode.equals(FagsakStatus.OPPRETTET.getKode())) {
            return Optional.of(PipFagsakStatus.OPPRETTET);
        }
        if (kode.equals(FagsakStatus.UNDER_BEHANDLING.getKode())) {
            return Optional.of(PipFagsakStatus.UNDER_BEHANDLING);
        }
        return Optional.empty();
    }

    public static Optional<PipBehandlingStatus> oversettBehandlingStatus(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        if (kode.equals(BehandlingStatus.OPPRETTET.getKode())) {
            return Optional.of(PipBehandlingStatus.OPPRETTET);
        }
        if (kode.equals(BehandlingStatus.UTREDES.getKode())) {
            return Optional.of(PipBehandlingStatus.UTREDES);
        }
        if (kode.equals(BehandlingStatus.FATTER_VEDTAK.getKode())) {
            return Optional.of(PipBehandlingStatus.FATTE_VEDTAK);
        }
        return Optional.empty();
    }

    private AbacUtil() {
        // util class
    }

}
