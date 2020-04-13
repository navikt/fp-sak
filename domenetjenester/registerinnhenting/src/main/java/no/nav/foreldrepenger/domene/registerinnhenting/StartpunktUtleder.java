package no.nav.foreldrepenger.domene.registerinnhenting;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public interface StartpunktUtleder {
    StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);

    default StartpunktType utledInitieltStartpunktRevurdering(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return utledStartpunkt(ref, grunnlagId1, grunnlagId2);
    }
}
