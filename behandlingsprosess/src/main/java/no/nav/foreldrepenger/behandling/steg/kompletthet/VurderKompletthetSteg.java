package no.nav.foreldrepenger.behandling.steg.kompletthet;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

public interface VurderKompletthetSteg extends BehandlingSteg {

    default boolean skalPassereKompletthet(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(BehandlingÅrsakType.årsakerRelatertTilDød()::contains);
    }

    default boolean kanPassereKompletthet(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(BehandlingÅrsakType.RE_HENDELSE_FØDSEL::equals);
    }
}
