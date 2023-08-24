package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;

import java.util.Collections;
import java.util.Set;

@ApplicationScoped
@GrunnlagRef(FamilieHendelseGrunnlagEntitet.ENTITY_NAME)
class BehandlingÅrsakUtlederFamilieHendelse implements BehandlingÅrsakUtleder {

    @Inject
    public BehandlingÅrsakUtlederFamilieHendelse() {
        //For CDI
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        return Collections.singleton(EndringResultatType.REGISTEROPPLYSNING);
    }
}
