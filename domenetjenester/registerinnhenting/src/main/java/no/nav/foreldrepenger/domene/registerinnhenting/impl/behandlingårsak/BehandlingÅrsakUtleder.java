package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

import java.util.Set;

public interface BehandlingÅrsakUtleder {
    Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);
}
