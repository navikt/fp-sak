package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.MEDLEM_GRUNNLAG)
class BehandlingÅrsakUtlederMedlemskap implements BehandlingÅrsakUtleder {

    @Inject
    public BehandlingÅrsakUtlederMedlemskap() {
        //For CDI
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Set.of();
    }
}
