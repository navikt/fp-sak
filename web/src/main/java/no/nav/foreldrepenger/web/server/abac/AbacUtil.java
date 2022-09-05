package no.nav.foreldrepenger.web.server.abac;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;

public final class AbacUtil {

    public static Optional<AbacFagsakStatus> oversettFagstatus(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        if (kode.equals(FagsakStatus.OPPRETTET.getKode())) {
            return Optional.of(AbacFagsakStatus.OPPRETTET);
        }
        if (kode.equals(FagsakStatus.UNDER_BEHANDLING.getKode())) {
            return Optional.of(AbacFagsakStatus.UNDER_BEHANDLING);
        }
        return Optional.empty();
    }

    public static Optional<AbacBehandlingStatus> oversettBehandlingStatus(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        if (kode.equals(BehandlingStatus.OPPRETTET.getKode())) {
            return Optional.of(AbacBehandlingStatus.OPPRETTET);
        }
        if (kode.equals(BehandlingStatus.UTREDES.getKode())) {
            return Optional.of(AbacBehandlingStatus.UTREDES);
        }
        if (kode.equals(BehandlingStatus.FATTER_VEDTAK.getKode())) {
            return Optional.of(AbacBehandlingStatus.FATTE_VEDTAK);
        }
        return Optional.empty();
    }

    public static Optional<no.nav.vedtak.sikkerhet.abac.pdp.FagsakStatus> oversettFagstatusNy(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        if (kode.equals(FagsakStatus.OPPRETTET.getKode())) {
            return Optional.of(no.nav.vedtak.sikkerhet.abac.pdp.FagsakStatus.OPPRETTET);
        }
        if (kode.equals(FagsakStatus.UNDER_BEHANDLING.getKode())) {
            return Optional.of(no.nav.vedtak.sikkerhet.abac.pdp.FagsakStatus.UNDER_BEHANDLING);
        }
        return Optional.empty();
    }

    public static Optional<no.nav.vedtak.sikkerhet.abac.pdp.BehandlingStatus> oversettBehandlingStatusNy(String kode) {
        if (kode == null) {
            return Optional.empty();
        }
        if (kode.equals(BehandlingStatus.OPPRETTET.getKode())) {
            return Optional.of(no.nav.vedtak.sikkerhet.abac.pdp.BehandlingStatus.OPPRETTET);
        }
        if (kode.equals(BehandlingStatus.UTREDES.getKode())) {
            return Optional.of(no.nav.vedtak.sikkerhet.abac.pdp.BehandlingStatus.UTREDES);
        }
        if (kode.equals(BehandlingStatus.FATTER_VEDTAK.getKode())) {
            return Optional.of(no.nav.vedtak.sikkerhet.abac.pdp.BehandlingStatus.FATTE_VEDTAK);
        }
        return Optional.empty();
    }

    private AbacUtil() {
        // util class
    }

}
