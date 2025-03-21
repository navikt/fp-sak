package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.YTELSE_FORDELING_GRUNNLAG)
class BehandlingÅrsakUtlederYtelseFordeling implements BehandlingÅrsakUtleder {

    public BehandlingÅrsakUtlederYtelseFordeling() {
        //For CDI
    }
}
