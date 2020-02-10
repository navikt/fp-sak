package no.nav.foreldrepenger.behandling.es;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;

public class UtledBehandlingResultatType {

    private UtledBehandlingResultatType() {
        // skal ikke instansieres
    }

    public static BehandlingResultatType utled(Behandlingsresultat behandlingsresultat) {
        Objects.requireNonNull(behandlingsresultat, "behandlingsresultat"); //NOSONAR
        return behandlingsresultat.isVilkårAvslått() ? BehandlingResultatType.AVSLÅTT : BehandlingResultatType.INNVILGET;
    }
}
