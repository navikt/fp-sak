package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;

@ApplicationScoped
@GrunnlagRef(FamilieHendelseGrunnlagEntitet.ENTITY_NAME)
class BehandlingÅrsakUtlederFamilieHendelse implements BehandlingÅrsakUtleder {

    public BehandlingÅrsakUtlederFamilieHendelse() {
        //For CDI
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        return Collections.singleton(EndringResultatType.REGISTEROPPLYSNING);
    }
}
