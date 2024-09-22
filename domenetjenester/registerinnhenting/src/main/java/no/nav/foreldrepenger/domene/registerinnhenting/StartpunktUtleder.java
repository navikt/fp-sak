package no.nav.foreldrepenger.domene.registerinnhenting;

import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public interface StartpunktUtleder {
    StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2);

    default Set<StartpunktType> utledInitieltStartpunktRevurdering(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        return Set.of(utledStartpunkt(ref, stp, grunnlagId1, grunnlagId2));
    }
}
