package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;

@ApplicationScoped
@GrunnlagRef(NesteSakGrunnlagEntitet.GRUNNLAG_NAME)
class BehandlingÅrsakUtlederYtelseFordeling implements BehandlingÅrsakUtleder {

    public BehandlingÅrsakUtlederYtelseFordeling() {
        //For CDI
    }
}
