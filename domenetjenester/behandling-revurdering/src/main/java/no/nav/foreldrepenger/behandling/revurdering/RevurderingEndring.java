package no.nav.foreldrepenger.behandling.revurdering;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;

public interface RevurderingEndring {

    boolean erRevurderingMedUendretUtfall(Behandling behandling, BehandlingResultatType nyResultatType);

    /**
     * Tjeneste som vurderer om revurderingen har endret utrfall i forhold til original behandling
     *
     * @param behandling
     * @return
     */
    boolean erRevurderingMedUendretUtfall(Behandling behandling);
}
