package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

public interface BehandlingÅrsakUtleder {
    Set<BehandlingÅrsakType> utledBehandlingÅrsaker(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);
}
