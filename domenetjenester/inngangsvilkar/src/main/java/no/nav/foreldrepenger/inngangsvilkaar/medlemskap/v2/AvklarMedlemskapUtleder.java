package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

@ApplicationScoped
public class AvklarMedlemskapUtleder {

    private MedlemRegelGrunnlagBygger grunnlagBygger;

    @Inject
    AvklarMedlemskapUtleder(MedlemRegelGrunnlagBygger grunnlagBygger) {
        this.grunnlagBygger = grunnlagBygger;
    }

    AvklarMedlemskapUtleder() {
        //CDI
    }

    public Set<MedlemskapAksjonspunktÅrsak> utledForInngangsvilkår(BehandlingReferanse behandlingRef) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlagInngangsvilkår(behandlingRef);
        return MedlemInngangsvilkårRegel.kjørRegler(grunnlag);
    }

    public Set<MedlemskapAksjonspunktÅrsak> utledForFortsattMedlem(BehandlingReferanse behandlingRef) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlagFortsattMedlem(behandlingRef);
        return MedlemFortsattRegel.kjørRegler(grunnlag);
    }

}
