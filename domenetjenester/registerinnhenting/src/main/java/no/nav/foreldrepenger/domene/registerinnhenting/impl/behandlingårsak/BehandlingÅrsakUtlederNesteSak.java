package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.YTELSE_FORDELING_GRUNNLAG)
class BehandlingÅrsakUtlederNesteSak implements BehandlingÅrsakUtleder {

    @Inject
    public BehandlingÅrsakUtlederNesteSak() {
        //For CDI
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Collections.emptySet();
    }
}
