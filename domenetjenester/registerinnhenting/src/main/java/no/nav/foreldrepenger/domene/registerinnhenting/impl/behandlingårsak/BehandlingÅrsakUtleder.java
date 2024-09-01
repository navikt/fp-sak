package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface BehandlingÅrsakUtleder {
    default Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        return Set.of();
    }
}
