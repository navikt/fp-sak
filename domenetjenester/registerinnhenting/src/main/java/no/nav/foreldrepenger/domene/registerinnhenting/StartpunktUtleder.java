package no.nav.foreldrepenger.domene.registerinnhenting;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public interface StartpunktUtleder {
    StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);

    default boolean erBehovForStartpunktUtledning(EndringsresultatDiff diff) {
        return diff.erSporedeFeltEndret();
    }
}
