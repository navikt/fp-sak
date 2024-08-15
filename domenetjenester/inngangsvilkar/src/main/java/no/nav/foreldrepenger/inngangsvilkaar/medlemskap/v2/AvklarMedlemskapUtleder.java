package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

@ApplicationScoped
public class AvklarMedlemskapUtleder {

    private MedlemskapsvilkårRegelGrunnlagBygger grunnlagBygger;

    @Inject
    AvklarMedlemskapUtleder(MedlemskapsvilkårRegelGrunnlagBygger grunnlagBygger) {
        this.grunnlagBygger = grunnlagBygger;
    }

    AvklarMedlemskapUtleder() {
        //CDI
    }

    public Set<MedlemskapAksjonspunktÅrsak> utledFor(BehandlingReferanse behandlingRef) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlag(behandlingRef);
        return MedlemskapsvilkårRegel.kjørRegler(grunnlag);
    }
}
