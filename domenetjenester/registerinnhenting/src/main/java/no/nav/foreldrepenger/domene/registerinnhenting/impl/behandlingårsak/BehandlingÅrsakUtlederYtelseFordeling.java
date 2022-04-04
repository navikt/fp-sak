package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.YTELSE_FORDELING_GRUNNLAG)
class BehandlingÅrsakUtlederYtelseFordeling implements BehandlingÅrsakUtleder {

    @Inject
    public BehandlingÅrsakUtlederYtelseFordeling() {
        //For CDI
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Collections.emptySet();
    }
}
