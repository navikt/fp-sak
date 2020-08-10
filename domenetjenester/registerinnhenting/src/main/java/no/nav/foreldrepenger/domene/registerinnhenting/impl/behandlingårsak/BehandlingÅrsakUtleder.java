package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public interface BehandlingÅrsakUtleder {
    Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);
}
