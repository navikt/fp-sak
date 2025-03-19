package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;

@ApplicationScoped
@GrunnlagRef(AktivitetskravGrunnlagEntitet.GRUNNLAG_NAME)
class BehandlingÅrsakUtlederAktivitetskravArbeid implements BehandlingÅrsakUtleder {

    public BehandlingÅrsakUtlederAktivitetskravArbeid() {
        //For CDI
    }
}
