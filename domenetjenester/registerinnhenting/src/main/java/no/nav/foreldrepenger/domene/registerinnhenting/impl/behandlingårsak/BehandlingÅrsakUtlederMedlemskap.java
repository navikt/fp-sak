package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.MEDLEM_GRUNNLAG)
class BehandlingÅrsakUtlederMedlemskap implements BehandlingÅrsakUtleder {

    public BehandlingÅrsakUtlederMedlemskap() {
        //For CDI
    }
}
