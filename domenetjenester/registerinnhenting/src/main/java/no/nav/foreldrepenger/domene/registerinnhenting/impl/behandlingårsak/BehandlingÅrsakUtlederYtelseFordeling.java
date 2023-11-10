package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;

@ApplicationScoped
@GrunnlagRef(NesteSakGrunnlagEntitet.GRUNNLAG_NAME)
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
