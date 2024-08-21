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

    public Set<MedlemskapAksjonspunktÅrsak> utledForInngangsvilkår(BehandlingReferanse behandlingRef) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlagInngangsvilkår(behandlingRef);
        return getKjørRegler(grunnlag);
    }

    public Set<MedlemskapAksjonspunktÅrsak> utledForFortsattMedlem(BehandlingReferanse behandlingRef) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlagFortsattMedlem(behandlingRef);
        return getKjørRegler(grunnlag);
    }

    private static Set<MedlemskapAksjonspunktÅrsak> getKjørRegler(MedlemskapsvilkårRegelGrunnlag grunnlag) {
        return MedlemskapsvilkårRegel.kjørRegler(grunnlag);
    }
}
