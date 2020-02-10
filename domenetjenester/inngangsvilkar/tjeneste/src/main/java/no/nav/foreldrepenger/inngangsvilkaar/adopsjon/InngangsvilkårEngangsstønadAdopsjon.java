package no.nav.foreldrepenger.inngangsvilkaar.adopsjon;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårTypeKoder;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårEngangsstønad;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;

@ApplicationScoped
@VilkårTypeRef(VilkårTypeKoder.FP_VK_4)
public class InngangsvilkårEngangsstønadAdopsjon implements Inngangsvilkår {

    private InngangsvilkårOversetter inngangsvilkårOversetter;

    InngangsvilkårEngangsstønadAdopsjon() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårEngangsstønadAdopsjon(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        AdopsjonsvilkårGrunnlag grunnlag = inngangsvilkårOversetter.oversettTilRegelModellAdopsjon(ref);

        Evaluation evaluation = new AdopsjonsvilkårEngangsstønad().evaluer(grunnlag);

        return inngangsvilkårOversetter.tilVilkårData(VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, evaluation, grunnlag);
    }
}
