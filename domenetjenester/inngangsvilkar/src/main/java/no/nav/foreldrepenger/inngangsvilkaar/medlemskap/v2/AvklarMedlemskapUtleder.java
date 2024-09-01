package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

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

    public Set<MedlemskapAksjonspunktÅrsak> utledForInngangsvilkår(BehandlingReferanse behandlingRef, Skjæringstidspunkt stp) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlagInngangsvilkår(behandlingRef, stp);
        return MedlemInngangsvilkårRegel.kjørRegler(grunnlag);
    }

    public Set<MedlemskapAksjonspunktÅrsak> utledForFortsattMedlem(BehandlingReferanse behandlingRef, Skjæringstidspunkt stp) {
        var grunnlag = grunnlagBygger.lagRegelGrunnlagFortsattMedlem(behandlingRef, stp);
        return MedlemFortsattRegel.kjørRegler(grunnlag);
    }

}
