package no.nav.foreldrepenger.behandling.steg.foreslåresultat;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;

public interface ForeslåBehandlingsresultatTjeneste {

    Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref);

}
