package no.nav.foreldrepenger.web.server.abac;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;

public final class AbacUtil {

    public static Optional<AbacFagsakStatus> oversettFagstatus(String kode) {
        if (kode.equals(FagsakStatus.OPPRETTET.getKode())) {
            return Optional.of(AbacFagsakStatus.OPPRETTET);
        } else if (kode.equals(FagsakStatus.UNDER_BEHANDLING.getKode())) {
            return Optional.of(AbacFagsakStatus.UNDER_BEHANDLING);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<AbacBehandlingStatus> oversettBehandlingStatus(String kode) {
        if (kode.equals(BehandlingStatus.OPPRETTET.getKode())) {
            return Optional.of(AbacBehandlingStatus.OPPRETTET);
        } else if (kode.equals(BehandlingStatus.UTREDES.getKode())) {
            return Optional.of(AbacBehandlingStatus.UTREDES);
        } else if (kode.equals(BehandlingStatus.FATTER_VEDTAK.getKode())) {
            return Optional.of(AbacBehandlingStatus.FATTE_VEDTAK);
        } else {
            return Optional.empty();
        }
    }
    
    private AbacUtil() {
        // util class
    }

}
