package no.nav.foreldrepenger.behandling.steg.kompletthet;

import java.util.Set;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

public interface VurderKompletthetSteg extends BehandlingSteg {
    Set<BehandlingÅrsakType> PASSER_KOMPLETTHET_HENDELSER = Set.of(BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL,
        BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN);

    default boolean skalPassereKompletthet(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(PASSER_KOMPLETTHET_HENDELSER::contains);
    }

    default boolean kanPassereKompletthet(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream()
            .map(BehandlingÅrsak::getBehandlingÅrsakType)
            .anyMatch(BehandlingÅrsakType.RE_HENDELSE_FØDSEL::equals);
    }
}
